package edu.hitsz.music;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;
import edu.hitsz.R;

public class PlaySoundManager {
    private Context context;
    private boolean musicEnabled;
    private MediaPlayer bgmPlayer, bgmBossPlayer;
    private SoundPool soundPool;
    private int soundBulletHit, soundBombExplosion, soundGameOver, soundGetSupply;

    public PlaySoundManager(Context context, boolean musicEnabled) {
        this.context = context.getApplicationContext();
        this.musicEnabled = musicEnabled;

        if (musicEnabled) {
            bgmPlayer = MediaPlayer.create(context, R.raw.bgm);
            bgmPlayer.setLooping(true);
            bgmBossPlayer = MediaPlayer.create(context, R.raw.bgm_boss);
            bgmBossPlayer.setLooping(true);

            AudioAttributes attrs = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            soundPool = new SoundPool.Builder()
                    .setMaxStreams(5)
                    .setAudioAttributes(attrs)
                    .build();
            soundBulletHit = soundPool.load(context, R.raw.bullet_hit, 1);
            soundBombExplosion = soundPool.load(context, R.raw.bomb_explosion, 1);
            soundGameOver = soundPool.load(context, R.raw.game_over, 1);
            soundGetSupply = soundPool.load(context, R.raw.get_supply, 1);
        }
    }

    public void playBgm() {
        if (musicEnabled && bgmPlayer != null && !bgmPlayer.isPlaying())
            bgmPlayer.start();
    }

    public void stopBgm() {
        if (bgmPlayer != null && bgmPlayer.isPlaying()) {
            bgmPlayer.pause();
            bgmPlayer.seekTo(0);
        }
    }

    public void playBgmBoss() {
        if (musicEnabled && bgmBossPlayer != null && !bgmBossPlayer.isPlaying())
            bgmBossPlayer.start();
    }

    public void stopBgmBoss() {
        if (bgmBossPlayer != null && bgmBossPlayer.isPlaying()) {
            bgmBossPlayer.pause();
            bgmBossPlayer.seekTo(0);
        }
    }

    public void playBulletHit() {
        if (musicEnabled && soundPool != null)
            soundPool.play(soundBulletHit, 1, 1, 0, 0, 1);
    }

    public void playBombExplosion() {
        if (musicEnabled && soundPool != null)
            soundPool.play(soundBombExplosion, 1, 1, 0, 0, 1);
    }

    public void playGameOver() {
        if (musicEnabled && soundPool != null)
            soundPool.play(soundGameOver, 1, 1, 0, 0, 1);
    }

    public void playGetSupply() {
        if (musicEnabled && soundPool != null)
            soundPool.play(soundGetSupply, 1, 1, 0, 0, 1);
    }

    public void shutdown() {
        if (bgmPlayer != null) { bgmPlayer.release(); bgmPlayer = null; }
        if (bgmBossPlayer != null) { bgmBossPlayer.release(); bgmBossPlayer = null; }
        if (soundPool != null) { soundPool.release(); soundPool = null; }
    }
}