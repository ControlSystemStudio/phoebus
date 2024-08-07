package org.csstudio.display.builder.model;

import static javax.swing.JOptionPane.YES_NO_OPTION;
import static javax.swing.JOptionPane.YES_OPTION;
import static javax.swing.JOptionPane.showConfirmDialog;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.csstudio.display.builder.model.AdvancedConverter.ConvertType;
import org.csstudio.display.builder.model.AdvancedConverter.ConverterEvent;
import org.csstudio.display.builder.model.AdvancedConverter.IConverterListener;

public class BrowserConverter extends JFrame implements IConverterListener {

	private static final long serialVersionUID = 1L;

	private static final String TITLE_WINDOW = "BOB Converter";
	private static final String OVERRIDING_MSG = "Are you sure to override these files :";
	private static final String LABEL_NAME_OUTPUT = "Output";
	private static final String LABEL_NAME_INPUT = "Input";
	private static final String ICON_HELP = "/icons/help.png";
	private static final String SELECT_NULL = "Select Paths";
	private static final String SELECT_A_FOLDER_OUTPUT = "select a folder who will receive the conversion";
	private static final String SELECT_FOLDER_INPUT = "select a folder or a file to convert";
	private static final Color VERY_DARK_GREEN = new Color(0,102,0);
	  public static final Color VERY_DARK_RED = new Color(153,0,0);



	private static BrowserConverter instance = null;
	private int progress = 0;
	private String[] fileList = null;
	private static File output;
	private static File input;
	private int returnvalue;
	private static String[] args;
	private ArrayList<File> over;

	// Message JDialog

	private static JDialog dlg;
	private static JLabel progressMsg;

	// HMI
	private JFileChooser fileChooserInput;
	private JFileChooser fileChooserOutput;
	private JTextField txtFieldInput;
	private JTextField txtFieldOutput;

	// process bar
	private JProgressBar b;

	// input/output list
	private static ArrayList<File> outputFolderList;
	private static ArrayList<File> inputFolderList;

	// error JDialog
	private static JDialog errDlg;
	private static JTextArea errorArea;
	private static JScrollPane scroll;

	private BrowserConverter() {
		over = new ArrayList<>();
		inputFolderList = new ArrayList<>();
		outputFolderList = new ArrayList<>();

		errorArea = new JTextArea();

		// Set Listener to converter
		AdvancedConverter.getInstance().setConverterListener(this);

		// creation of the HMI
		createHIM();

	}

	/**
	 * 
	 * This method create the HIM
	 */
	public void createHIM() {
		JPanel window = new JPanel();
		setTitle(TITLE_WINDOW);
		setSize(410, 142);
		setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		txtFieldInput = new JTextField(17);
		JButton inputBut = new JButton("Browse");
		inputBut.setPreferredSize(new Dimension(80, 30));
		txtFieldOutput = new JTextField(17);
		JButton outputBut = new JButton("Browse");
		outputBut.setPreferredSize(new Dimension(80, 30));
		JButton runBut = new JButton("Run");
		runBut.setPreferredSize(new Dimension(400, 30));
		URL resource = BrowserConverter.class.getResource(ICON_HELP);
		Icon help = new ImageIcon(resource);
		JLabel helpLabelInput = new JLabel(help);
		JLabel helpLabelOutput = new JLabel(help);
		helpLabelInput.setPreferredSize(new Dimension(25, 25));
		helpLabelOutput.setPreferredSize(new Dimension(25, 25));
		txtFieldInput.setEditable(false);
		txtFieldOutput.setEditable(false);
		JLabel inLabel = new JLabel(LABEL_NAME_INPUT);
		helpLabelInput.setToolTipText(SELECT_FOLDER_INPUT);
		inLabel.setToolTipText(SELECT_FOLDER_INPUT);
		JLabel outLabel = new JLabel(LABEL_NAME_OUTPUT);
		outLabel.setToolTipText(SELECT_A_FOLDER_OUTPUT);
		helpLabelOutput.setToolTipText(SELECT_A_FOLDER_OUTPUT);
		inLabel.setPreferredSize(new Dimension(55, 15));
		outLabel.setPreferredSize(new Dimension(55, 15));

		// filtre1 = new JCheckBox("resize OPI");
		// filtre2 = new JCheckBox("Filter 2");
		// filtre3 = new JCheckBox("Filer 3");
		// filtre4 = new JCheckBox("Filter 4");

		window.add(inLabel);
		window.add(helpLabelInput);
		window.add(txtFieldInput);
		window.add(inputBut);
		window.add(outLabel);
		window.add(helpLabelOutput);
		window.add(txtFieldOutput);
		window.add(outputBut);
		/*
		 * window.add(filtre1); window.add(filtre2); window.add(filtre3);
		 * window.add(filtre4);
		 */
		window.add(runBut);

		add(window);
		setResizable(false);

		// open input selection if inputBut pressed
		inputBut.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					selectInput();
				} catch (IOException ex) {
					throw new RuntimeException(ex);
				}

			}
		});

		// open output selection if outputBut pressed
		outputBut.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					selectOutput();
				} catch (IOException ex) {
					throw new RuntimeException(ex);
				}

			}
		});

		// Run button to start conversion
		runBut.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				reset();
				if (input != null && output != null) {
					try {
						if (JOptionPane.showConfirmDialog(instance,
								"Are you sure to convert " + input + " into " + output, "Confirm",
								YES_NO_OPTION) == YES_OPTION) {
							override(input, output);
							convert();
						} else {
							return;
						}
					} catch (IOException ex) {
						throw new RuntimeException(ex);
					}
				} else if (input != null && output == null) {
					output = input;
					try {
						if (JOptionPane.showConfirmDialog(instance,
								"Are you sure to convert " + input + " into " + output, "Confirm",
								YES_NO_OPTION) == YES_OPTION) {
							convert();
						} else {
							return;
						}
					} catch (IOException ex) {
						throw new RuntimeException(ex);
					}
				} else if (input == null && output == null) {
					JOptionPane.showMessageDialog(null, SELECT_NULL);
				} else if (input == output) {

					try {
						if (JOptionPane.showConfirmDialog(instance,
								"Are you sure to convert " + input + " into " + output, "Confirm",
								YES_NO_OPTION) == YES_OPTION) {
							convert();
						}
					} catch (IOException ex) {
						throw new RuntimeException(ex);
					}
				} else if (input.listFiles() == null) {
					try {
						if (JOptionPane.showConfirmDialog(null, "Are you sure to convert " + input + " into " + output,
								"Confirm", YES_NO_OPTION) == YES_OPTION) {
							convert();
						}
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
			}

		});
	}

	/**
	 * 
	 * This method open the input file or folder selection
	 */
	private void selectInput() throws IOException {
		if (fileChooserInput == null) {
			fileChooserInput = new JFileChooser();
			fileChooserInput.setDialogTitle(LABEL_NAME_INPUT);
			fileChooserInput.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		}
		returnvalue = fileChooserInput.showOpenDialog(instance);
		if (returnvalue == JFileChooser.APPROVE_OPTION) {
			input = fileChooserInput.getSelectedFile();
			txtFieldInput.setText(input.getAbsolutePath());
		}
	}

	/**
	 * 
	 * This method open the output folder selection
	 */
	private void selectOutput() throws IOException {
		if (fileChooserOutput == null) {
			fileChooserOutput = new JFileChooser();
			fileChooserOutput.setDialogTitle(LABEL_NAME_OUTPUT);
			fileChooserOutput.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		}
		int returnvalue = fileChooserOutput.showOpenDialog(instance);
		if (returnvalue == JFileChooser.APPROVE_OPTION) {
			output = fileChooserOutput.getSelectedFile();
			txtFieldOutput.setText(output.getAbsolutePath());
		}
	}

	/**
	 * return every opi files in the input folder
	 * 
	 * @param input
	 * @return inputFolderList
	 */
	protected ArrayList<File> getInputFile(File input) {

		if (input.listFiles() != null) {
			for (final File fileEntry : Objects.requireNonNull(input.listFiles())) {
				if (fileEntry.isDirectory()) {
					getInputFile(fileEntry);
				} else {
					if (AdvancedConverter.getInstance().isOpiFile(fileEntry.getName())) {
						System.out.println("input " + fileEntry.getName());
						inputFolderList.add(fileEntry);

					}
				}
			}
		}
		return inputFolderList;

	}

	/**
	 * return an ArrayList with every bob files in the output folder
	 * 
	 * @param output
	 * @return outputFolderList
	 */
	protected ArrayList<File> getOutputFile(File output) {
		if (output.listFiles() != null) {
			for (final File fileExit : Objects.requireNonNull(output.listFiles())) {
				if (fileExit.isDirectory()) {
					getOutputFile(fileExit);
				} else {
					if (AdvancedConverter.getInstance().isBobFile(fileExit.getName())) {
						System.out.println("output " + fileExit.getName());
						outputFolderList.add(fileExit);

					}
				}
			}
		}
		return outputFolderList;

	}

	/**
	 * This method seek for similar files in the input and output folder
	 * 
	 * @param input
	 * @param output
	 * @throws IOException
	 * 
	 * 
	 */
	public void override(File input, File output) throws IOException {
		// research all same files in the input and output
		ArrayList<File> folder1 = getInputFile(input);
		ArrayList<File> folder2 = getOutputFile(output);

		for (int i = 0; i < folder1.size(); i++) {
			for (int j = 0; j < folder2.size(); j++) {
				if (folder2.get(j).getName().substring(0, folder2.get(j).getName().length() - 4)
						.equals(folder1.get(i).getName().substring(0, folder1.get(i).getName().length() - 4))) {
					over.add(folder2.get(j));
				}
			}
		}

	}

	/**
	 * This method delete bob files in the output folder who already exist in the
	 * input folder and are present in the output folder then convert the input
	 * folder
	 * 
	 * @throws IOException
	 * 
	 * 
	 */
	public void convert() throws IOException {
		// delete all duplicate file to the output
		if (!over.isEmpty()) {
			if (showConfirmDialog(null, OVERRIDING_MSG + " \n" + over, "Conflict", YES_NO_OPTION) == YES_OPTION) {
				
				List<Path> pathList = new ArrayList<Path>();
				for(File file : over) {
					pathList.add(file.toPath());
				}
				
				for (Path path : pathList) {
					Files.delete(path);
				}
				
//				for (int i = 0; i < over.size(); i++) {
//					// System.out.println("im deleting " + over.get(i).toPath());
//					Files.delete(over.get(i).toPath());
//				}
			} else {
				return;
			}
		}
		// convert every files from the input
		displayProgress("Processing...");
		conversion();
	}

	/**
	 * This method display messages during the conversion
	 * 
	 * @param msgDisplay
	 * 
	 * 
	 */
	private void displayProgress(String msgDisplay) {

		// Create an instance of jdialog
		if (dlg == null) {
			dlg = new JDialog(dlg, "converting");
			dlg.setAlwaysOnTop(true);
			dlg.setSize(250, 150);

			progressMsg = new JLabel("init message");
			b = new JProgressBar(0, 100);
			dlg.add(progressMsg);
			b.setBounds(35, 40, 165, 30);
			progressMsg.setBounds(40, 25, 200, 15);
			b.setValue(0);
			dlg.setLayout(null);
			b.setStringPainted(true);
			dlg.add(b);

		}

		Thread displayThread = new Thread() {

			public void run() {
				dlg.setLocationRelativeTo(instance);
				dlg.setVisible(true);
				progressMsg.setText(msgDisplay);
				dlg.setCursor(new Cursor(Cursor.WAIT_CURSOR));

			};
		};

		// Call thread run method
		SwingUtilities.invokeLater(displayThread);
	}

	/**
	 * Thread conversion
	 * 
	 * This method execute the conversion and update the message displayed
	 */
	private void conversion() {
		Thread t = new Thread() {
			public void run() {
				args = new String[] { "-output ", output.getAbsolutePath(), input.getAbsolutePath() };
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
				}
				AdvancedConverter.getInstance().launchConversion(args);
				displayProgress("Process finish");
				try {
					Thread.sleep(1500);
				} catch (InterruptedException e) {
				}
				dlg.setVisible(false);
			}
		};
		t.start();
	}

	@Override
	public void convertEvent(ConverterEvent event) {
		ConvertType type = event.getType();
		System.out.println("convertEvent" + event.getType());
		System.out.println("File" + event.getFile());

		switch (type) {
		case FILELIST:
			fileList = event.getFileList();
			break;
		case NEWFILE:
			incrementProgress();
			// TODO
			displayProgress("Convert " + event.getFile().getName());
			progressMsg.setForeground(Color.BLACK);
			break;

		case ERROR:
			displayError(event.getMessage());
			displayProgress("Error script");
			progressMsg.setForeground(VERY_DARK_RED);
			break;
		
		case SUCCESS:
			displayProgress(event.getMessage());
			progressMsg.setForeground(VERY_DARK_GREEN);
		default:
			break;
		}

	}

	/**
	 * this method fill the progress bar
	 * 
	 * @param val(advancement
	 *            value)
	 */
	private void incrementProgress() {
		int percentage = 0;
		int nbFile = fileList != null ? fileList.length : 0;
		progress++;
		System.out.println("progress " + progress);
		if (nbFile == 0) {
			percentage = (progress * 100) / 1;
		} else {
			percentage = (progress * 100) / (nbFile-2);
		}
		b.setValue(percentage);

	}

	/**
	 * display error
	 * 
	 * @param msgError
	 */
	private void displayError(String msgError) {
		if (errDlg == null) {
			errDlg = new JDialog(errDlg, "Conversion error");
			errDlg.setAlwaysOnTop(true);
			errDlg.setSize(550, 150);
			errorArea.setEditable(false);
			scroll = new JScrollPane(errorArea);
		}
		errorArea.append(msgError + "\n");
		errDlg.add(scroll);
		errDlg.setLocationRelativeTo(null);
		errDlg.setVisible(true);

		// JOptionPane.showMessageDialog( null,msgError,
		// "Error",JOptionPane.WARNING_MESSAGE);
	}

	/**
	 * reset variable of progression and of overriding after conversion
	 */
	private void reset() {
		progress = 0;
		fileList = null;
		inputFolderList.clear();
		outputFolderList.clear();
		errorArea.setText("");
		over.clear();
		if(progressMsg != null || b!=null) {
			progressMsg.setForeground(Color.BLACK);
			b.setValue(0);

		}

	}

	public static void displayBrowserConverter() {
		if (instance == null) {
			instance = new BrowserConverter();
		}
		instance.setVisible(true);
		instance.setLocationRelativeTo(null);
		instance.toFront();
	}

	// Local test
	/*
	 * public static void main(String[] args){ try { new BrowserConvertor(); } catch
	 * (IOException e) { throw new RuntimeException(e); }
	 * 
	 * }
	 */

}
