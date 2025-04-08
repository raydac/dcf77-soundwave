import com.igormaznitsa.gui.AppFrame;
import javax.swing.SwingUtilities;

public class DCF77SoundStation {

  public static void main(final String... args) {
    AppFrame.ensureAppropriateLF();
    SwingUtilities.invokeLater(() -> {
      final AppFrame appFrame = new AppFrame();
      appFrame.setVisible(true);
    });
  }
}
