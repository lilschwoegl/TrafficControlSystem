package simulator;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class gridTest implements Runnable {

    private JFrame frame;

    @Override
    public void run() {
        frame = new JFrame("Grid Test");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());

        DrawingPanel drawingPanel = new DrawingPanel();
        mainPanel.add(drawingPanel);

        frame.add(mainPanel);

        frame.pack();
        frame.setLocationByPlatform(true);
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new gridTest());
    }

    public class DrawingPanel extends JPanel {

        private static final long serialVersionUID = -5711127036945010446L;

        private int width = 750; // 25 * 30
        private int height = 600; // 20 * 30

        public DrawingPanel() {
            this.setPreferredSize(new Dimension(width + 2, height + 2));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            int x = 1;
            int y = 1;
            int size = 30;

            for (int i = 0; i < 25; i++) {
                for (int j = 0; j < 20; j++) {
                    g.drawRect(x, y, size, size);
                    y += size;
                }
                x += size;
                y = 1;
            }
        }
    }

}