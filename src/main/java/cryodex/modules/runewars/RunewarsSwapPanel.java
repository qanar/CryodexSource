package cryodex.modules.runewars;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import cryodex.CryodexController;
import cryodex.Main;
import cryodex.Player;
import cryodex.widget.ComponentUtils;

@SuppressWarnings("serial")
public class RunewarsSwapPanel extends JPanel {

	public static void showSwapPanel() {
		JDialog manualModificationPanel = new JDialog(Main.getInstance(),
				"Swap Players", true);
		JPanel panel = new JPanel(new BorderLayout());
		manualModificationPanel.getContentPane().add(panel);
		panel.add(new RunewarsSwapPanel(manualModificationPanel),
				BorderLayout.CENTER);
		manualModificationPanel.setPreferredSize(new Dimension(400, 600));
		manualModificationPanel.pack();

		manualModificationPanel.setVisible(true);
	}

	private JButton swapButton;
	private JButton addButton;
	private JButton closeButton;

	private final RunewarsPlayer blankPlayer = new RunewarsPlayer(new Player());

	private final List<RunewarsMatch> matches;

	private final List<MatchPanel> matchPanels;
	private MatchPanel quickEntryMatch = null;

	private JPanel mainMatchPanel;
	private JPanel quickEntryPanel;
	private JPanel quickEntrySubPanel;
	private JTextField roundNumber;
	private JComboBox<RunewarsPlayer> playerCombo;

	private boolean updating = false;
	private final JDialog parent;

	public RunewarsSwapPanel(JDialog parent) {
		super(new BorderLayout());

		this.parent = parent;

		matches = new ArrayList<>();
		matches.addAll(((RunewarsTournament) CryodexController
				.getActiveTournament()).getSelectedRound().getMatches());

		matchPanels = new ArrayList<>();
		for (int counter = 0; counter < matches.size(); counter++) {
			matchPanels.add(new MatchPanel(matches.get(counter), counter + 1));
		}

		buildPanel();

		final JScrollPane checkboxScrollPanel = new JScrollPane(
				ComponentUtils.addToFlowLayout(getMainMatchPanel(),
						FlowLayout.LEFT));
		checkboxScrollPanel
				.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		checkboxScrollPanel.setBorder(BorderFactory.createEmptyBorder());

		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				checkboxScrollPanel.getVerticalScrollBar().setValue(0);
			}
		});

		JPanel buttonPanel = new JPanel(new FlowLayout());
		buttonPanel.add(getAddButton());
		buttonPanel.add(getSwapButton());
		buttonPanel.add(getCloseButton());

		this.add(getQuickEntryPanel(), BorderLayout.NORTH);
		this.add(checkboxScrollPanel, BorderLayout.CENTER);
		this.add(buttonPanel, BorderLayout.SOUTH);

		updatePanels();
	}

	public JPanel getQuickEntryPanel() {
		if (quickEntryPanel == null) {
			quickEntryPanel = new JPanel(new BorderLayout());
			quickEntryPanel.setVisible(CryodexController.getOptions()
					.isShowQuickFind());
			ComponentUtils.forceSize(quickEntryPanel, 405, 135);

			quickEntrySubPanel = new JPanel(new BorderLayout());
			ComponentUtils.forceSize(quickEntrySubPanel, 400, 130);

			roundNumber = new JTextField(5);

			roundNumber.getDocument().addDocumentListener(
					new DocumentListener() {
						@Override
						public void changedUpdate(DocumentEvent e) {
							update();
						}

						@Override
						public void removeUpdate(DocumentEvent e) {
							update();
						}

						@Override
						public void insertUpdate(DocumentEvent e) {
							update();
						}
					});

			List<RunewarsPlayer> playerList = new ArrayList<RunewarsPlayer>();

			playerList.add(new RunewarsPlayer(new Player()));
			playerList.addAll(((RunewarsTournament) CryodexController
					.getActiveTournament()).getRunewarsPlayers());

			Collections.sort(playerList);

			playerCombo = new JComboBox<RunewarsPlayer>(
					playerList.toArray(new RunewarsPlayer[playerList.size()]));

			playerCombo.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent arg0) {
					update();
				}
			});

			quickEntryPanel.add(ComponentUtils.addToFlowLayout(ComponentUtils
					.addToHorizontalBorderLayout(new JLabel(
							"Enter table number"), roundNumber, ComponentUtils
							.addToHorizontalBorderLayout(new JLabel(
									"or choose a player"), playerCombo, null)),
					FlowLayout.CENTER), BorderLayout.NORTH);

			quickEntryPanel.add(quickEntrySubPanel);
		}

		return quickEntryPanel;
	}

	public void update() {

		Integer i = null;
		try {
			i = Integer.parseInt(roundNumber.getText());
		} catch (NumberFormatException e) {

		}

		RunewarsPlayer player = playerCombo.getSelectedIndex() == 0 ? null
				: (RunewarsPlayer) playerCombo.getSelectedItem();

		if (player != null) {
			roundNumber.setEnabled(false);
		} else if (i != null) {
			playerCombo.setEnabled(false);
		} else {
			roundNumber.setEnabled(true);
			playerCombo.setEnabled(true);
		}

		MatchPanel matchPanel = null;
		if (i != null) {
			if (i <= matchPanels.size() && i > 0) {
				matchPanel = matchPanels.get(i - 1);
			}
		} else if (player != null) {
			for (MatchPanel mp : matchPanels) {
				if (mp.getPlayer1Combo().getSelectedItem() == player) {
					matchPanel = mp;
					break;
				} else if (mp.getPlayer2Combo().getSelectedItem() != blankPlayer
						&& mp.getPlayer2Combo().getSelectedItem() == player) {
					matchPanel = mp;
					break;
				}
			}
		}

		quickEntryMatch = matchPanel;

		quickEntrySubPanel.removeAll();
		getMainMatchPanel().removeAll();

		if (quickEntryMatch != null) {
			quickEntrySubPanel.add(quickEntryMatch);
		}

		buildPanel();

		ComponentUtils.repaint(quickEntrySubPanel);
		ComponentUtils.repaint(quickEntryPanel);
		ComponentUtils.repaint(getMainMatchPanel());
	}

	private JButton getSwapButton() {
		if (swapButton == null) {
			swapButton = new JButton("SWAP");
			swapButton.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {

					RunewarsTournament tournament = (RunewarsTournament) CryodexController
							.getActiveTournament();

					List<RunewarsMatch> matches = new ArrayList<RunewarsMatch>();

					boolean isSingleElimination = tournament.getLatestRound()
							.isSingleElimination();

					int roundNumber = tournament.getAllRounds().size();

					tournament.cancelRound(roundNumber);

					for (MatchPanel mp : matchPanels) {
						RunewarsMatch m = mp.getNewMatch();

						if (m != null) {
							m.checkDuplicate(tournament.getAllRounds());
							matches.add(m);
						}
					}

					RunewarsRound r = new RunewarsRound(matches, tournament,
							roundNumber);
					r.setSingleElimination(isSingleElimination);

					tournament.getAllRounds().add(r);
					if (isSingleElimination) {
						tournament
								.getTournamentGUI()
								.getRoundTabbedPane()
								.addSingleEliminationTab(
										r.getMatches().size() * 2, r.getPanel());
					} else {
						tournament.getTournamentGUI().getRoundTabbedPane()
								.addSwissTab(roundNumber, r.getPanel());
					}

					CryodexController.saveData();
				}
			});

		}
		return swapButton;
	}

	private JButton getAddButton() {
		if (addButton == null) {
			addButton = new JButton("Add Match");

			addButton.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					MatchPanel mp = new MatchPanel(new RunewarsMatch(),
							matchPanels.size());

					matchPanels.add(mp);

					buildPanel();

					updatePanels();
				}
			});
		}

		return addButton;
	}

	private JButton getCloseButton() {
		if (closeButton == null) {
			closeButton = new JButton("Close");

			closeButton.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					parent.setVisible(false);
				}
			});
		}

		return closeButton;
	}

	private JPanel getMainMatchPanel() {
		if (mainMatchPanel == null) {
			mainMatchPanel = new JPanel(new GridBagLayout());
		}

		return mainMatchPanel;
	}

	private void buildPanel() {

		getMainMatchPanel().removeAll();

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = -1;

		for (MatchPanel mp : matchPanels) {
			if (mp == quickEntryMatch) {
				continue;
			}
			gbc.gridy++;
			getMainMatchPanel().add(mp, gbc);
		}
	}

	private void updatePanels() {

		if (updating) {
			return;
		}

		updating = true;

		List<RunewarsPlayer> tempPlayers = new ArrayList<>();
		tempPlayers.addAll(((RunewarsTournament) CryodexController
				.getActiveTournament()).getRunewarsPlayers());

		for (MatchPanel mp : matchPanels) {
			tempPlayers.remove(mp.getPlayer1Combo().getSelectedItem());
			tempPlayers.remove(mp.getPlayer2Combo().getSelectedItem());
		}

		for (MatchPanel mp : matchPanels) {
			mp.updateCombos(tempPlayers);
		}

		updating = false;
	}

	private class MatchPanel extends JPanel {

		private JComboBox<RunewarsPlayer> player1Combo;
		private JComboBox<RunewarsPlayer> player2Combo;

		private final RunewarsMatch match;

		public MatchPanel(RunewarsMatch match, int tableNumber) {

			super(new FlowLayout(FlowLayout.CENTER));

			this.match = match;

			JPanel hPanel = ComponentUtils.addToHorizontalBorderLayout(
					getPlayer1Combo(), new JLabel("VS"), getPlayer2Combo());

			JPanel vPanel = ComponentUtils.addToVerticalBorderLayout(
					new JLabel("Table " + tableNumber), hPanel, null);

			this.add(vPanel);

		}

		public RunewarsMatch getNewMatch() {
			RunewarsPlayer p1 = (RunewarsPlayer) getPlayer1Combo()
					.getSelectedItem();
			RunewarsPlayer p2 = (RunewarsPlayer) getPlayer2Combo()
					.getSelectedItem();

			if (p1 == blankPlayer) {
				p1 = null;
			}

			if (p2 == blankPlayer) {
				p2 = null;
			}

			if (p1 == null) {
				p1 = p2;
				p2 = null;
			}

			if (p1 == null) {
				return null;
			}

			RunewarsMatch m = new RunewarsMatch(p1, p2);

			if (p2 == null) {
				m.setBye(true);
			}

			return m;
		}

		public JComboBox<RunewarsPlayer> getPlayer1Combo() {

			if (player1Combo == null) {
				player1Combo = new JComboBox<RunewarsPlayer>();
				ComponentUtils.forceSize(player1Combo, 100, 25);
				getPlayer1Combo().addItem(blankPlayer);
				if (match.getPlayer1() != null) {
					getPlayer1Combo().addItem(match.getPlayer1());
					getPlayer1Combo().setSelectedItem(match.getPlayer1());
				}
				player1Combo.addActionListener(new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent arg0) {
						updatePanels();
					}
				});
			}

			return player1Combo;
		}

		public JComboBox<RunewarsPlayer> getPlayer2Combo() {

			if (player2Combo == null) {
				player2Combo = new JComboBox<RunewarsPlayer>();
				ComponentUtils.forceSize(player2Combo, 100, 25);

				getPlayer2Combo().addItem(blankPlayer);
				if (match.getPlayer2() != null) {
					getPlayer2Combo().addItem(match.getPlayer2());
					getPlayer2Combo().setSelectedItem(match.getPlayer2());
				}
				player2Combo.addActionListener(new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent arg0) {
						updatePanels();
					}
				});
			}

			return player2Combo;
		}

		public void updateCombos(List<RunewarsPlayer> players) {

			RunewarsPlayer p1 = (RunewarsPlayer) getPlayer1Combo()
					.getSelectedItem();
			RunewarsPlayer p2 = (RunewarsPlayer) getPlayer2Combo()
					.getSelectedItem();

			getPlayer1Combo().removeAllItems();
			getPlayer2Combo().removeAllItems();

			List<RunewarsPlayer> list1 = new ArrayList<>();
			List<RunewarsPlayer> list2 = new ArrayList<>();

			if (p1 != blankPlayer) {
				list1.add(p1);
			}

			if (p2 != blankPlayer) {
				list2.add(p2);
			}

			for (RunewarsPlayer xp : players) {
				list1.add(xp);
				list2.add(xp);
			}

			Collections.sort(list1);
			Collections.sort(list2);

			getPlayer1Combo().addItem(blankPlayer);
			for (RunewarsPlayer xp : list1) {
				getPlayer1Combo().addItem(xp);
			}
			if (p1 != blankPlayer) {
				getPlayer1Combo().setSelectedItem(p1);
			} else {
				getPlayer1Combo().setSelectedItem(blankPlayer);
			}

			getPlayer2Combo().addItem(blankPlayer);
			for (RunewarsPlayer xp : list2) {
				getPlayer2Combo().addItem(xp);
			}
			if (p2 != blankPlayer) {
				getPlayer2Combo().setSelectedItem(p2);
			} else {
				getPlayer2Combo().setSelectedItem(blankPlayer);
			}
		}
	}
}
