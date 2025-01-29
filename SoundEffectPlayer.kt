import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.example.spaceseeker.R

object SoundEffectPlayer {
    private var soundPool: SoundPool? = null
    private var shootSoundId: Int = 0

    fun loadSounds(context: Context) {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(5)
            .setAudioAttributes(audioAttributes)
            .build()

        // Load sounds (Replace R.raw.shoot and R.raw.enemy_shoot with your actual .wav file names)
        shootSoundId = soundPool?.load(context, R.raw.shoot_effects, 1) ?: 0
    }

    fun playShootSound() {
        soundPool?.play(shootSoundId, 1f, 1f, 1, 0, 1f)
    }

    fun release() {
        soundPool?.release()
        soundPool = null
    }
}
