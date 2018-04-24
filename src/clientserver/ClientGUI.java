package clientserver;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

public class ClientGUI implements ActionListener {

	private String title;
	private int width;
	private int height;
	private int posX;
	private int posY;
	private JFrame frame;
	
	private JComboBox<String> queryType;
	private JButton requestQuery;
	private JTable results;
	private JScrollPane tableScrollPane;
	private JPanel controlPanel;
	
	public ClientGUI (String title, int width, int height) {
		this(title, width, height, 0, 0);
	}
	
	public ClientGUI (String title, int width, int height, int posX, int posY) {
		this.title = title;
		this.width = width;
		this.height = height;
		this.posX = posX;
		this.posY = posY;
		createDisplay();
	}
	
	public void createDisplay (){
		frame = new JFrame(title);
		frame.setSize(width, height);
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setLocationRelativeTo(null);
		frame.setLocation(posX, posY);
		frame.setLayout(new BorderLayout());
		
		controlPanel = new JPanel();
		controlPanel.setLayout(new BorderLayout());
		
		requestQuery = new JButton("Request From Server");
		requestQuery.setSize(200, 30);
		requestQuery.setLocation(frame.getWidth() / 2 - requestQuery.getWidth() / 2,
				frame.getHeight() - requestQuery.getHeight());
		requestQuery.addActionListener(this);
		
		queryType = new JComboBox<String>();
		queryType.addItem("EVENTS");
		queryType.addItem("METRICS");
		queryType.addItem("TRAFFIC");
		queryType.setSize(200, 30);
		queryType.setLocation(frame.getWidth() / 2 - requestQuery.getWidth() / 2,
				frame.getHeight() - requestQuery.getHeight() - queryType.getHeight());
		
		results = new JTable(300, 300);
		results.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		tableScrollPane = new JScrollPane(results);
		
		
		frame.add(tableScrollPane, BorderLayout.CENTER);
		controlPanel.add(queryType, BorderLayout.NORTH);
		controlPanel.add(requestQuery, BorderLayout.SOUTH);
		frame.add(controlPanel, BorderLayout.SOUTH);
		
		frame.pack();
	}
	
	public static void main (String[] args)
	{
		new ClientGUI("Client", 500, 500);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		
		if (e.getSource() == requestQuery)
		{
			ServerClient sc = new ServerClient();
			String queryTypeStr = queryType.getSelectedItem().toString();
			QueryMessage qm = sc.queryServer(queryTypeStr);
			DefaultTableModel model = new DefaultTableModel(qm.getColumns(), 0);
			String[][] rows = qm.getRows();
			
			results.setModel(model);
			
			for (String[] s : rows)
			{
				model.addRow(s);
			}
			
		}
		
	}

}
