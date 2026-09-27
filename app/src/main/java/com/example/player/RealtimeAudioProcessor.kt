package com.example.player

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.Visualizer
import android.os.SystemClock
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.sin
import kotlin.math.sqrt

enum class VisualizerSource {
    VISUALIZER_API,
    AUDIO_RECORD,
    SYNTHETIC
}

enum class VisualizerStyle {
    SPECTRUM_BARS,
    RADIAL_MANDALA,
    WAVEFORM_CURVE
}

class RealtimeAudioProcessor(
    private val context: Context,
    private val bandCount: Int = 36
) {
    private val tag = "RealtimeAudioProcessor"
    private val scope = CoroutineScope(Dispatchers.Default + Job())

    private var visualizer: Visualizer? = null
    private var audioRecord: AudioRecord? = null
    private var audioRecordJob: Job? = null
    private var fallbackJob: Job? = null

    private val _frequencyBands = MutableStateFlow(FloatArray(bandCount))
    val frequencyBands: StateFlow<FloatArray> = _frequencyBands.asStateFlow()

    private val _peakBands = MutableStateFlow(FloatArray(bandCount))
    val peakBands: StateFlow<FloatArray> = _peakBands.asStateFlow()

    private val _waveformData = MutableStateFlow(FloatArray(bandCount * 2))
    val waveformData: StateFlow<FloatArray> = _waveformData.asStateFlow()

    private val _activeSource = MutableStateFlow(VisualizerSource.SYNTHETIC)
    val activeSource: StateFlow<VisualizerSource> = _activeSource.asStateFlow()

    private val _sensitivity = MutableStateFlow(1.2f)
    val sensitivity: StateFlow<Float> = _sensitivity.asStateFlow()

    private val _currentStyle = MutableStateFlow(VisualizerStyle.SPECTRUM_BARS)
    val currentStyle: StateFlow<VisualizerStyle> = _currentStyle.asStateFlow()

    // Peak decay velocities for smooth gravity fall
    private val peakDropSpeeds = FloatArray(bandCount) { 0f }
    private val smoothedFrequencies = FloatArray(bandCount) { 0f }

    private var currentSessionId: Int = 0
    private var isPlayingAudio: Boolean = false
    private var preferredSource: VisualizerSource = VisualizerSource.VISUALIZER_API

    init {
        startDecayAndFallbackLoop()
    }

    fun setStyle(style: VisualizerStyle) {
        _currentStyle.value = style
    }

    fun setSensitivity(gain: Float) {
        _sensitivity.value = gain.coerceIn(0.5f, 3.0f)
    }

    fun setPlaybackState(isPlaying: Boolean, sessionId: Int) {
        this.isPlayingAudio = isPlaying
        if (this.currentSessionId != sessionId && sessionId > 0) {
            this.currentSessionId = sessionId
            if (hasRecordPermission()) {
                attachVisualizer(sessionId)
            }
        }
    }

    fun hasRecordPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun onPermissionGranted() {
        if (preferredSource == VisualizerSource.VISUALIZER_API && currentSessionId > 0) {
            attachVisualizer(currentSessionId)
        } else if (preferredSource == VisualizerSource.AUDIO_RECORD) {
            startAudioRecordCapture()
        }
    }

    fun switchSource(source: VisualizerSource) {
        preferredSource = source
        releaseHardwareCapture()
        when (source) {
            VisualizerSource.VISUALIZER_API -> {
                if (hasRecordPermission() && currentSessionId > 0) {
                    attachVisualizer(currentSessionId)
                } else {
                    _activeSource.value = VisualizerSource.SYNTHETIC
                }
            }
            VisualizerSource.AUDIO_RECORD -> {
                if (hasRecordPermission()) {
                    startAudioRecordCapture()
                } else {
                    _activeSource.value = VisualizerSource.SYNTHETIC
                }
            }
            VisualizerSource.SYNTHETIC -> {
                _activeSource.value = VisualizerSource.SYNTHETIC
            }
        }
    }

    private fun attachVisualizer(sessionId: Int) {
        try {
            releaseHardwareCapture()
            val captureSize = Visualizer.getCaptureSizeRange()[1].coerceAtMost(512)
            val vis = Visualizer(sessionId).apply {
                this.captureSize = captureSize
                setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(
                        vis: Visualizer?,
                        waveform: ByteArray?,
                        samplingRate: Int
                    ) {
                        waveform?.let { processWaveform(it) }
                    }

                    override fun onFftDataCapture(
                        vis: Visualizer?,
                        fft: ByteArray?,
                        samplingRate: Int
                    ) {
                        fft?.let { processVisualizerFft(it) }
                    }
                }, Visualizer.getMaxCaptureRate() / 2, true, true)
                enabled = true
            }
            visualizer = vis
            _activeSource.value = VisualizerSource.VISUALIZER_API
            Log.d(tag, "Attached Visualizer to audio session: $sessionId")
        } catch (e: Exception) {
            Log.w(tag, "Could not initialize Visualizer (device/emulator limitation): ${e.message}")
            _activeSource.value = VisualizerSource.SYNTHETIC
        }
    }

    private fun processVisualizerFft(fft: ByteArray) {
        if (!isPlayingAudio) {
            return
        }

        val rawBands = FloatArray(bandCount)
        val n = fft.size / 2 // number of frequency components
        val gain = _sensitivity.value

        for (i in 0 until bandCount) {
            // Logarithmic mapping to human hearing frequency distribution
            val minBin = ((i.toFloat() / bandCount) * (i.toFloat() / bandCount) * (n - 1)).toInt()
            val maxBin = (((i + 1).toFloat() / bandCount) * ((i + 1).toFloat() / bandCount) * (n - 1)).toInt().coerceAtLeast(minBin + 1)

            var sum = 0f
            var count = 0
            for (k in minBin until maxBin.coerceAtMost(n)) {
                val re = fft[2 * k].toFloat()
                val im = if (2 * k + 1 < fft.size) fft[2 * k + 1].toFloat() else 0f
                val mag = hypot(re, im)
                sum += mag
                count++
            }

            val avg = if (count > 0) sum / count else 0f
            // Normalize with mild logarithmic dynamic range compression
            val normalized = (avg / 128f * gain).coerceIn(0f, 1f)
            rawBands[i] = normalized
        }

        updateSmoothedAndPeaks(rawBands)
    }

    private fun processWaveform(waveform: ByteArray) {
        val count = _waveformData.value.size
        val points = FloatArray(count)
        val step = (waveform.size.toFloat() / count).coerceAtLeast(1f)

        for (i in 0 until count) {
            val idx = (i * step).toInt().coerceIn(0, waveform.size - 1)
            // waveform contains unsigned 8-bit PCM (0 to 255 with 128 as silence)
            val byteVal = waveform[idx].toInt() and 0xFF
            points[i] = (byteVal - 128f) / 128f
        }
        _waveformData.value = points
    }

    private fun startAudioRecordCapture() {
        if (!hasRecordPermission()) return
        try {
            val sampleRate = 44100
            val channelConfig = AudioFormat.CHANNEL_IN_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT
            val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
                .coerceAtLeast(1024)

            val recorder = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            if (recorder.state != AudioRecord.STATE_INITIALIZED) {
                Log.w(tag, "AudioRecord could not be initialized")
                recorder.release()
                _activeSource.value = VisualizerSource.SYNTHETIC
                return
            }

            recorder.startRecording()
            audioRecord = recorder
            _activeSource.value = VisualizerSource.AUDIO_RECORD

            audioRecordJob?.cancel()
            audioRecordJob = scope.launch {
                val shortBuffer = ShortArray(512)
                val gain = _sensitivity.value
                while (isActive) {
                    val read = recorder.read(shortBuffer, 0, shortBuffer.size)
                    if (read > 0) {
                        // Compute energy bands from PCM short buffer
                        val bands = FloatArray(bandCount)
                        val step = (read.toFloat() / bandCount).toInt().coerceAtLeast(1)
                        for (i in 0 until bandCount) {
                            var sum = 0f
                            val start = i * step
                            val end = ((i + 1) * step).coerceAtMost(read)
                            for (j in start until end) {
                                val sample = shortBuffer[j] / 32768f
                                sum += sample * sample
                            }
                            val rms = sqrt(sum / (end - start).coerceAtLeast(1))
                            bands[i] = (rms * 3.5f * gain).coerceIn(0f, 1f)
                        }
                        updateSmoothedAndPeaks(bands)
                    }
                    delay(30)
                }
            }
        } catch (e: Exception) {
            Log.w(tag, "AudioRecord exception: ${e.message}")
            _activeSource.value = VisualizerSource.SYNTHETIC
        }
    }

    private fun startDecayAndFallbackLoop() {
        fallbackJob?.cancel()
        fallbackJob = scope.launch {
            var phase = 0f
            while (isActive) {
                delay(25) // ~40 FPS physics & animation update
                phase += 0.12f

                if (_activeSource.value == VisualizerSource.SYNTHETIC || !isPlayingAudio) {
                    val syntheticBands = FloatArray(bandCount)
                    val wavePoints = FloatArray(_waveformData.value.size)

                    if (isPlayingAudio) {
                        val gain = _sensitivity.value
                        // Generate dynamic, realistic musical frequency spectra
                        for (i in 0 until bandCount) {
                            val f = (i + 1).toFloat() / bandCount
                            // Bass prominence (bands 0..6)
                            val bassPulse = if (i < 8) {
                                (sin(phase * 1.8f) * 0.4f + cos(phase * 0.9f) * 0.3f + 0.35f) * (1.0f - f * 0.5f)
                            } else 0f

                            // Mid-frequency rhythm
                            val midRhythm = (sin(phase * 2.5f + i * 0.5f) * 0.3f + cos(phase * 1.2f + i * 0.2f) * 0.2f + 0.3f)

                            // High-frequency shimmer
                            val highShimmer = (sin(phase * 4.0f + i * 0.8f) * 0.2f + 0.2f) * (f * 0.9f)

                            val combined = (bassPulse + midRhythm * 0.7f + highShimmer) * gain
                            syntheticBands[i] = combined.coerceIn(0.05f, 0.98f)
                        }

                        // Generate smooth waveform points
                        for (w in wavePoints.indices) {
                            val t = w.toFloat() / wavePoints.size
                            wavePoints[w] = (sin(phase * 2f + t * 12f) * 0.4f + sin(phase * 3.5f + t * 24f) * 0.25f)
                        }
                    } else {
                        // Idle gentle breathing animation
                        for (i in 0 until bandCount) {
                            syntheticBands[i] = 0.05f + (sin(phase * 0.6f + i * 0.2f) * 0.03f)
                        }
                    }

                    _waveformData.value = wavePoints
                    updateSmoothedAndPeaks(syntheticBands)
                } else {
                    // Update peak decay for active hardware captures
                    decayPeaksOnly()
                }
            }
        }
    }

    private fun updateSmoothedAndPeaks(raw: FloatArray) {
        val smoothed = FloatArray(bandCount)
        val peaks = _peakBands.value.copyOf()

        val smoothingFactor = 0.45f
        val gravity = 0.015f

        for (i in 0 until bandCount) {
            val target = raw.getOrElse(i) { 0f }
            // Smooth attack and decay
            smoothedFrequencies[i] = (smoothedFrequencies[i] * (1f - smoothingFactor)) + (target * smoothingFactor)
            smoothed[i] = smoothedFrequencies[i]

            // Peak hold & gravity physics
            if (smoothed[i] >= peaks[i]) {
                peaks[i] = smoothed[i]
                peakDropSpeeds[i] = 0f
            } else {
                peakDropSpeeds[i] += gravity
                peaks[i] = (peaks[i] - peakDropSpeeds[i]).coerceAtLeast(0f)
            }
        }

        _frequencyBands.value = smoothed
        _peakBands.value = peaks
    }

    private fun decayPeaksOnly() {
        val peaks = _peakBands.value.copyOf()
        val gravity = 0.015f
        var changed = false
        for (i in 0 until bandCount) {
            val curr = smoothedFrequencies[i]
            if (peaks[i] > curr) {
                peakDropSpeeds[i] += gravity
                peaks[i] = (peaks[i] - peakDropSpeeds[i]).coerceAtLeast(curr)
                changed = true
            }
        }
        if (changed) {
            _peakBands.value = peaks
        }
    }

    private fun releaseHardwareCapture() {
        try {
            visualizer?.enabled = false
            visualizer?.release()
            visualizer = null
        } catch (e: Exception) {
            Log.e(tag, "Error releasing visualizer", e)
        }

        try {
            audioRecordJob?.cancel()
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
        } catch (e: Exception) {
            Log.e(tag, "Error releasing audioRecord", e)
        }
    }

    fun release() {
        releaseHardwareCapture()
        fallbackJob?.cancel()
    }
}
