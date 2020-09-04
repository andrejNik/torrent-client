package nikonov.torrentclient.gui.domain.player;

import javafx.scene.image.ImageView;
import nikonov.torrentclient.gui.domain.TorrentFile;
import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;
import static uk.co.caprica.vlcj.javafx.videosurface.ImageViewVideoSurfaceFactory.videoSurfaceForImageView;

/**
 * Плеер на основе vlcj-библиотеки
 */
public class VLCJPlayer implements Player {

    private final MediaPlayerFactory mediaPlayerFactory;
    private final EmbeddedMediaPlayer mediaPlayer;

    public VLCJPlayer(ImageView imageView) {
        this.mediaPlayerFactory = new MediaPlayerFactory();
        this.mediaPlayer = mediaPlayerFactory.mediaPlayers().newEmbeddedMediaPlayer();
        this.mediaPlayer.videoSurface().set(videoSurfaceForImageView(imageView));
    }

    @Override
    public void play(TorrentFile file) {
        // mediaPlayer.media().play("C:\\Users\\andre\\Desktop\\84.mkv");
    }

    @Override
    public void destroy() {
        mediaPlayer.controls().stop();
        mediaPlayer.release();
        mediaPlayerFactory.release();
    }
}
