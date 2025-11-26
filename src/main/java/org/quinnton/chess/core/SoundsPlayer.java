package org.quinnton.chess.core;

import org.quinnton.chess.Main;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.io.InputStream;
import java.util.Objects;

public class SoundsPlayer {
    /**
     *
     * @param url name of sound file
     */
    public static synchronized void playSound(final String url) {
        new Thread(new Runnable() {
            public void run() {
                try {
                    Clip clip = AudioSystem.getClip();
                    InputStream stream = Main.class.getResourceAsStream("/sounds/" + url);
                    if (stream == null) {
                        System.err.println("SOUND NOT FOUND: " + url);
                        return;
                    }
                    AudioInputStream inputStream = AudioSystem.getAudioInputStream(stream);
                            Objects.requireNonNull(getClass().getResourceAsStream("/sounds/" + url));
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
