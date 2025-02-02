/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2010 - 2023 TrackMate developers.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
package fiji.plugin.trackmate.gui.wizard;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridLayout;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.border.EmptyBorder;

import fiji.plugin.trackmate.gui.GuiUtils;
import fiji.plugin.trackmate.gui.wizard.TransitionAnimator.Direction;
import fiji.plugin.trackmate.util.PainterThread;
import fiji.plugin.trackmate.util.PainterThread.Paintable;

public class WizardPanel extends JPanel
{
	private static final long serialVersionUID = 1L;

	public static final Font FONT = new Font( "Arial", Font.PLAIN, 10 );

	public static final Font BIG_FONT = new Font( "Arial", Font.PLAIN, 14 );

	public static final Font SMALL_FONT = FONT.deriveFont( 8 );

	private final CardLayout cardLayout;

	private final AnimatorPanel animatorPanel;

	final JPanel panelMain;

	final JButton btnSave;

	final JToggleButton btnLog;

	final JToggleButton btnDisplayConfig;

	final JButton btnPrevious;

	final JButton btnNext;

	final JButton btnCancel;

	final JButton btnResume;

	final JPanel panelButtons;

	public WizardPanel()
	{
		setLayout( new BorderLayout( 0, 0 ) );
		this.animatorPanel = new AnimatorPanel();

		this.panelButtons = new JPanel();
		panelButtons.setBorder( new EmptyBorder( 3, 3, 3, 3 ) );
		add( panelButtons, BorderLayout.SOUTH );
		panelButtons.setLayout( new BoxLayout( panelButtons, BoxLayout.X_AXIS ) );

		this.btnSave = new JButton();
		panelButtons.add( btnSave );

		final Component horizontalGlue_1 = Box.createHorizontalGlue();
		panelButtons.add( horizontalGlue_1 );

		this.btnLog = new JToggleButton();
		panelButtons.add( btnLog );

		btnDisplayConfig = new JToggleButton();
		panelButtons.add( btnDisplayConfig );

		final Component horizontalGlue = Box.createHorizontalGlue();
		panelButtons.add( horizontalGlue );

		this.btnPrevious = new JButton();
		panelButtons.add( btnPrevious );

		btnNext = new JButton();
		panelButtons.add( btnNext );

		this.btnCancel = new JButton();
		panelButtons.add( btnCancel );

		btnResume = new JButton();
		panelButtons.add( btnResume );

		this.panelMain = new JPanel();
		add( panelMain, BorderLayout.CENTER );
		this.cardLayout = new CardLayout( 0, 0 );
		panelMain.setLayout( cardLayout );
	}

	public void display( final WizardPanelDescriptor current )
	{
		panelMain.add( current.getPanelComponent(), current.getPanelDescriptorIdentifier() );
		cardLayout.show( panelMain, current.getPanelDescriptorIdentifier() );
	}

	public void transition( final WizardPanelDescriptor to, final WizardPanelDescriptor from, final Direction direction )
	{
		animatorPanel.start( from, to, direction );
	}

	private class AnimatorPanel extends JPanel implements Paintable
	{
		private static final long serialVersionUID = 1L;

		private static final long duration = 200; // ms

		private final JLabel label;

		private TransitionAnimator animator;

		private WizardPanelDescriptor to;

		private final PainterThread painterThread;

		public AnimatorPanel()
		{
			super( new GridLayout() );
			this.label = new JLabel();
			add( label );
			this.painterThread = new PainterThread( this );
			painterThread.start();

			GuiUtils.addOnClosingEvent( this, () -> animatorPanel.painterThread.interrupt() );
		}

		public void start( final WizardPanelDescriptor from, final WizardPanelDescriptor to, final Direction direction )
		{
			this.to = to;
			this.animator = new TransitionAnimator( from.getPanelComponent(), to.getPanelComponent(), direction, duration );
			label.setIcon( new ImageIcon( animator.getCurrent( System.currentTimeMillis() ) ) );

			panelMain.add( this, "transitionCard" );
			cardLayout.show( panelMain, "transitionCard" );
		}

		private void stop()
		{
			animator = null;
			panelMain.remove( this );
			panelMain.add( to.getPanelComponent(), to.getPanelDescriptorIdentifier() );
			cardLayout.show( panelMain, to.getPanelDescriptorIdentifier() );
		}

		@Override
		public void paint()
		{
			synchronized ( this )
			{
				if ( animator != null )
				{
					label.setIcon( new ImageIcon( animator.getCurrent( System.currentTimeMillis() ) ) );
					if ( animator.isComplete() )
						stop();

					repaint();
				}
			}
		}

		@Override
		public void repaint()
		{
			if ( null != painterThread )
				painterThread.requestRepaint();
		}
	}
}
