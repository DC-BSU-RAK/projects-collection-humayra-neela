package com.aura.auraflows

import android.media.MediaPlayer
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    // Global variables for views
    private lateinit var welcomeLayout: ConstraintLayout
    private lateinit var homeLayout: ConstraintLayout
    private lateinit var infoOverlay: ConstraintLayout
    private lateinit var musicPlayerLayout: CardView
    private lateinit var moodButtons: List<ToggleButton>
    private lateinit var myMoodMusicButton: Button
    private lateinit var resetButton: Button
    private lateinit var playPauseButton: ImageButton
    private lateinit var muteButton: ImageButton
    private lateinit var songDetailText: TextView

    private var mediaPlayer: MediaPlayer? = null
    private var isMuted = false

    // List to maintain a consistent order for mood comparison
    private val moodOrder by lazy {
        listOf(
            R.id.moodHappy,
            R.id.moodSad,
            R.id.moodAngry,
            R.id.moodEmotional,
            R.id.moodFrustrated,
            R.id.moodExcited
        )
    }

    // Song info structure
    data class Song(val title: String, val artist: String, val resName: String)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Handle window insets for edge-to-edge
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initViews()
    }

    // Find views and set click listeners
    private fun initViews() {
        welcomeLayout = findViewById(R.id.welcomeLayout)
        homeLayout = findViewById(R.id.homeLayout)
        infoOverlay = findViewById(R.id.infoOverlay)
        musicPlayerLayout = findViewById(R.id.musicPlayerLayout)
        myMoodMusicButton = findViewById(R.id.myMoodMusicButton)
        resetButton = findViewById(R.id.resetButton)
        playPauseButton = findViewById(R.id.playPauseButton)
        muteButton = findViewById(R.id.muteButton)
        songDetailText = findViewById(R.id.songDetailText)

        // Show info screen
        findViewById<ImageButton>(R.id.infoButton).setOnClickListener {
            showInfoOverlay()
        }

        // Close info screen
        findViewById<Button>(R.id.closeInfoButton).setOnClickListener {
            hideInfoOverlay()
        }

        // Welcome to home screen transition
        findViewById<Button>(R.id.getStartedButton).setOnClickListener {
            transitionToHome()
        }

        // Home back to welcome transition
        findViewById<ImageButton>(R.id.backToWelcomeButton).setOnClickListener {
            transitionToWelcome()
        }

        // Mood selection buttons
        moodButtons = listOf(
            findViewById(R.id.moodHappy),
            findViewById(R.id.moodSad),
            findViewById(R.id.moodAngry),
            findViewById(R.id.moodEmotional),
            findViewById(R.id.moodFrustrated),
            findViewById(R.id.moodExcited)
        )

        moodButtons.forEach { button ->
            button.setOnClickListener {
                handleMoodSelection(button)
            }
        }

        // Play music based on selected moods
        myMoodMusicButton.setOnClickListener {
            recommendAndPlaySong()
        }

        // Clear all moods and stop player
        resetButton.setOnClickListener {
            resetMoods()
            stopMusic()
            musicPlayerLayout.visibility = View.GONE
        }

        // Player controls
        playPauseButton.setOnClickListener { togglePlayPause() }
        findViewById<ImageButton>(R.id.replayButton).setOnClickListener { restartSong() }
        muteButton.setOnClickListener { toggleMute() }

        updateMyMoodMusicButtonState()
    }

    private fun showInfoOverlay() {
        val slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up)
        infoOverlay.visibility = View.VISIBLE
        infoOverlay.startAnimation(slideUp)
    }

    private fun hideInfoOverlay() {
        val fadeOut = AnimationUtils.loadAnimation(this, R.anim.fade_out)
        fadeOut.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
            override fun onAnimationStart(animation: android.view.animation.Animation?) {}
            override fun onAnimationEnd(animation: android.view.animation.Animation?) {
                infoOverlay.visibility = View.GONE
            }
            override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}
        })
        infoOverlay.startAnimation(fadeOut)
    }

    private fun transitionToHome() {
        val fadeOut = AnimationUtils.loadAnimation(this, R.anim.fade_out)
        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)

        welcomeLayout.startAnimation(fadeOut)
        welcomeLayout.visibility = View.GONE

        homeLayout.visibility = View.VISIBLE
        homeLayout.startAnimation(fadeIn)
    }

    private fun transitionToWelcome() {
        val fadeOut = AnimationUtils.loadAnimation(this, R.anim.fade_out)
        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)

        stopMusic()
        homeLayout.startAnimation(fadeOut)
        homeLayout.visibility = View.GONE

        welcomeLayout.visibility = View.VISIBLE
        welcomeLayout.startAnimation(fadeIn)
        
        resetMoods()
        musicPlayerLayout.visibility = View.GONE
    }

    private fun resetMoods() {
        moodButtons.forEach { it.isChecked = false }
        updateMyMoodMusicButtonState()
    }

    // Limit selection to exactly 2 moods
    private fun handleMoodSelection(clickedButton: ToggleButton) {
        val selectedButtons = moodButtons.filter { it.isChecked }
        if (selectedButtons.size > 2) {
            clickedButton.isChecked = false
            Toast.makeText(this, getString(R.string.select_limit_toast), Toast.LENGTH_SHORT).show()
        }
        updateMyMoodMusicButtonState()
    }

    private fun updateMyMoodMusicButtonState() {
        val selectedCount = moodButtons.count { it.isChecked }
        myMoodMusicButton.isEnabled = selectedCount == 2
    }

    // Main logic for song recommendation
    private fun recommendAndPlaySong() {
        // Sort selected moods based on fixed order to ensure unique matching
        val selectedIds = moodButtons
            .filter { it.isChecked }
            .map { it.id }
            .sortedBy { id -> moodOrder.indexOf(id) }

        if (selectedIds.size == 2) {
            val song = getSongForCombination(selectedIds)
            
            // Set song title and show player
            songDetailText.text = "${song.title} - ${song.artist}"
            musicPlayerLayout.visibility = View.VISIBLE
            
            playMusic(song.resName)
        }
    }

    // MediaPlayer setup and play
    private fun playMusic(resName: String) {
        stopMusic()
        
        val resId = resources.getIdentifier(resName, "raw", packageName)
        if (resId == 0) {
            Toast.makeText(this, "Audio file not found: $resName", Toast.LENGTH_LONG).show()
            return
        }

        try {
            mediaPlayer = MediaPlayer.create(this, resId)
            mediaPlayer?.isLooping = true
            if (isMuted) mediaPlayer?.setVolume(0f, 0f)
            mediaPlayer?.start()
            playPauseButton.setImageResource(android.R.drawable.ic_media_pause)
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to play song", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopMusic() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        playPauseButton.setImageResource(android.R.drawable.ic_media_play)
    }

    private fun togglePlayPause() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                playPauseButton.setImageResource(android.R.drawable.ic_media_play)
            } else {
                it.start()
                playPauseButton.setImageResource(android.R.drawable.ic_media_pause)
            }
        }
    }

    private fun restartSong() {
        mediaPlayer?.seekTo(0)
        mediaPlayer?.start()
        playPauseButton.setImageResource(android.R.drawable.ic_media_pause)
    }

    private fun toggleMute() {
        isMuted = !isMuted
        mediaPlayer?.let {
            if (isMuted) {
                it.setVolume(0f, 0f)
                muteButton.setImageResource(android.R.drawable.ic_lock_silent_mode)
            } else {
                it.setVolume(1f, 1f)
                muteButton.setImageResource(android.R.drawable.ic_lock_silent_mode_off)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopMusic()
    }

    // Mapping of mood pairs to 15 different songs
    private fun getSongForCombination(ids: List<Int>): Song {
        val pair = ids[0] to ids[1]
        
        return when (pair) {
            // Happy pairs
            (R.id.moodHappy to R.id.moodSad) -> Song("Bittersweet Symphony", "The Verve", "song_1")
            (R.id.moodHappy to R.id.moodAngry) -> Song("Mr. Brightside", "The Killers", "song_2")
            (R.id.moodHappy to R.id.moodEmotional) -> Song("Yellow", "Coldplay", "song_3")
            (R.id.moodHappy to R.id.moodFrustrated) -> Song("Shake It Off", "Taylor Swift", "song_4")
            (R.id.moodHappy to R.id.moodExcited) -> Song("Happy", "Pharrell Williams", "song_5")
            
            // Sad pairs
            (R.id.moodSad to R.id.moodAngry) -> Song("In the End", "Linkin Park", "song_6")
            (R.id.moodSad to R.id.moodEmotional) -> Song("Someone Like You", "Adele", "song_7")
            (R.id.moodSad to R.id.moodFrustrated) -> Song("Boulevard of Broken Dreams", "Green Day", "song_8")
            (R.id.moodSad to R.id.moodExcited) -> Song("Counting Stars", "OneRepublic", "song_9")
            
            // Angry pairs
            (R.id.moodAngry to R.id.moodEmotional) -> Song("Elastic Heart", "Sia", "song_10")
            (R.id.moodAngry to R.id.moodFrustrated) -> Song("Killing In The Name", "Rage Against The Machine", "song_11")
            (R.id.moodAngry to R.id.moodExcited) -> Song("Thunderstruck", "AC/DC", "song_12")
            
            // Emotional pairs
            (R.id.moodEmotional to R.id.moodFrustrated) -> Song("Numb", "Linkin Park", "song_13")
            (R.id.moodEmotional to R.id.moodExcited) -> Song("Dog Days Are Over", "Florence + The Machine", "song_14")
            
            // Frustrated pair
            (R.id.moodFrustrated to R.id.moodExcited) -> Song("Uprising", "Muse", "song_15")
            
            else -> Song("The Nights", "Avicii", "song_1")
        }
    }
}
