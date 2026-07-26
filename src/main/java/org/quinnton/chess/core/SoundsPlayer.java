package org.quinnton.chess.core;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.io.InputStream;

public class SoundsPlayer {

    /**
     * Whether sounds actually play. Headless / test runs disable this so the
     * move path never touches the audio subsystem.
     */
    private static volatile boolean enabled = true;

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    /**
     *
     * @param url name of sound file
     */
    public static synchronized void playSound(final String url) {
        if (!enabled) return;
        new Thread(new Runnable() {
            public void run() {
                try {
                    Clip clip = AudioSystem.getClip();
                    InputStream stream = SoundsPlayer.class.getResourceAsStream("/sounds/" + url);
                    if (stream == null) {
                        System.err.println("SOUND NOT FOUND: " + url);
                        return;
                    }
                    AudioInputStream inputStream = AudioSystem.getAudioInputStream(stream);
                    clip.open(inputStream);
                    clip.start();
                } catch (Exception e) {
                    System.out.print("Error playing sound: ");
                    System.err.println(e.getMessage());
                }
            }
        }).start();
    }


    public static void playMoveSelfSound(){
        playSound("move-self.wav");
    }

    public static void playPromoteSound(){
        playSound("promote.wav");
    }

    public static void playCaptureSound(){
        playSound("capture.wav");
    }

    public static void playMoveCheckSound(){
        playSound("move-check.wav");
    }

    public static void playCastleSound(){
        playSound("castle.wav");
    }
}
