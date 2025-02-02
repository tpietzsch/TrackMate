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
package fiji.plugin.trackmate.features.track;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.HashSet;
import java.util.TreeSet;

import org.junit.Before;
import org.junit.Test;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.ModelChangeEvent;
import fiji.plugin.trackmate.ModelChangeListener;
import fiji.plugin.trackmate.Spot;

public class TrackBranchingAnalyzerTest
{

	private static final int N_LINEAR_TRACKS = 5;

	private static final int N_TRACKS_WITH_GAPS = 7;

	private static final int N_TRACKS_WITH_SPLITS = 9;

	private static final int N_TRACKS_WITH_MERGES = 11;

	private static final int DEPTH = 9;

	private Model model;

	private Spot split;

	private Spot lastSpot1;

	private Spot lastSpot2;

	private Spot firstSpot;

	@Before
	public void setUp()
	{
		model = new Model();
		model.beginUpdate();
		try
		{
			// linear tracks
			for ( int i = 0; i < N_LINEAR_TRACKS; i++ )
			{
				Spot previous = null;
				for ( int j = 0; j < DEPTH; j++ )
				{
					final Spot spot = new Spot( 0d, 0d, 0d, 1d, -1d );
					model.addSpotTo( spot, j );
					if ( null != previous )
					{
						model.addEdge( previous, spot, 1 );
					}
					previous = spot;
				}
			}
			// tracks with gaps
			for ( int i = 0; i < N_TRACKS_WITH_GAPS; i++ )
			{
				Spot previous = null;
				for ( int j = 0; j < DEPTH; j++ )
				{
					if ( j == DEPTH / 2 )
					{
						continue;
					}
					final Spot spot = new Spot( 0d, 0d, 0d, 1d, -1d );
					model.addSpotTo( spot, j );
					if ( null != previous )
					{
						model.addEdge( previous, spot, 1 );
					}
					previous = spot;
				}
			}
			// tracks with splits
			for ( int i = 0; i < N_TRACKS_WITH_SPLITS; i++ )
			{
				Spot previous = null;
				split = null; // Store the spot at the branch split
				for ( int j = 0; j < DEPTH; j++ )
				{
					final Spot spot = new Spot( 0d, 0d, 0d, 1d, -1d );
					if ( j == DEPTH / 2 )
					{
						split = spot;
					}
					model.addSpotTo( spot, j );
					if ( null != previous )
					{
						model.addEdge( previous, spot, 1 );
					}
					else
					{
						firstSpot = spot; // Store first spot of track
					}
					previous = spot;
				}
				lastSpot1 = previous; // Store last spot of 1st branch
				previous = split;
				for ( int j = DEPTH / 2 + 1; j < DEPTH; j++ )
				{
					final Spot spot = new Spot( 0d, 0d, 0d, 1d, -1d );
					model.addSpotTo( spot, j );
					model.addEdge( previous, spot, 1 );
					previous = spot;
				}
				lastSpot2 = previous; // Store last spot of 2nd branch
			}
			// tracks with merges
			for ( int i = 0; i < N_TRACKS_WITH_MERGES; i++ )
			{
				Spot previous = null;
				Spot merge = null;
				for ( int j = 0; j < DEPTH; j++ )
				{
					final Spot spot = new Spot( 0d, 0d, 0d, 1d, -1d );
					if ( j == DEPTH / 2 )
					{
						merge = spot;
					}
					model.addSpotTo( spot, j );
					if ( null != previous )
					{
						model.addEdge( previous, spot, 1 );
					}
					previous = spot;
				}
				previous = null;
				for ( int j = 0; j < DEPTH / 2; j++ )
				{
					final Spot spot = new Spot( 0d, 0d, 0d, 1d, -1d );
					model.addSpotTo( spot, j );
					if ( null != previous )
					{
						model.addEdge( previous, spot, 1 );
					}
					previous = spot;
				}
				model.addEdge( previous, merge, 1 );
			}

		}
		finally
		{
			model.endUpdate();
		}
	}

	@Test
	public final void testProcess()
	{
		// Process model
		final TrackBranchingAnalyzer analyzer = new TrackBranchingAnalyzer();
		analyzer.process( model.getTrackModel().trackIDs( true ), model );

		// Collect features
		int nTracksWithGaps = 0;
		int nTracksWithSplits = 0;
		int nTracksWithMerges = 0;
		int nTracksWithNothing = 0;
		for ( final Integer trackID : model.getTrackModel().trackIDs( true ) )
		{
			nTracksWithGaps += model.getFeatureModel().getTrackFeature( trackID, TrackBranchingAnalyzer.NUMBER_GAPS );
			nTracksWithMerges += model.getFeatureModel().getTrackFeature( trackID, TrackBranchingAnalyzer.NUMBER_MERGES );
			nTracksWithSplits += model.getFeatureModel().getTrackFeature( trackID, TrackBranchingAnalyzer.NUMBER_SPLITS );
			if ( 0 == model.getFeatureModel().getTrackFeature( trackID, TrackBranchingAnalyzer.NUMBER_GAPS ) && 0 == model.getFeatureModel().getTrackFeature( trackID, TrackBranchingAnalyzer.NUMBER_MERGES ) && 0 == model.getFeatureModel().getTrackFeature( trackID, TrackBranchingAnalyzer.NUMBER_SPLITS ) )
			{
				nTracksWithNothing++;
			}
		}
		assertEquals( N_LINEAR_TRACKS, nTracksWithNothing );
		assertEquals( N_TRACKS_WITH_GAPS, nTracksWithGaps );
		assertEquals( N_TRACKS_WITH_MERGES, nTracksWithMerges );
		assertEquals( N_TRACKS_WITH_SPLITS, nTracksWithSplits );
	}

	@Test
	public final void testModelChanged()
	{
		// Copy old keys
		final HashSet< Integer > oldKeys = new HashSet<>( model.getTrackModel().trackIDs( true ) );

		// First analysis
		final TestTrackBranchingAnalyzer analyzer = new TestTrackBranchingAnalyzer( model );
		analyzer.process( oldKeys, model );

		// Reset analyzer
		analyzer.hasBeenCalled = false;
		analyzer.keys = null;

		// Prepare listener for model change
		final ModelChangeListener listener = new ModelChangeListener()
		{
			@Override
			public void modelChanged( final ModelChangeEvent event )
			{
				analyzer.process( event.getTrackUpdated(), model );
			}
		};
		model.addModelChangeListener( listener );

		// Add a new track to the model - the old tracks should not be affected
		model.beginUpdate();
		try
		{
			final Spot spot1 = model.addSpotTo( new Spot( 0d, 0d, 0d, 1d, -1d ), 0 );
			final Spot spot2 = model.addSpotTo( new Spot( 0d, 0d, 0d, 1d, -1d ), 1 );
			model.addEdge( spot1, spot2, 1 );

		}
		finally
		{
			model.endUpdate();
		}

		// The analyzer must have done something:
		assertTrue( analyzer.hasBeenCalled );

		// Check the track IDs the analyzer received - none of the old keys must
		// be in it
		for ( final Integer calledKey : analyzer.keys )
		{
			if ( oldKeys.contains( calledKey ) )
			{
				fail( "Track with ID " + calledKey + " should not have been re-analyzed." );
			}
		}

		// Reset analyzer
		analyzer.hasBeenCalled = false;
		analyzer.keys = null;

		// New change: graft a new spot on the first track - it should be
		// re-analyzed
		final Integer firstKey = oldKeys.iterator().next();
		final Spot lFirstSpot = model.getTrackModel().trackSpots( firstKey ).iterator().next();
		Spot newSpot = null;
		final int firstFrame = lFirstSpot.getFeature( Spot.FRAME ).intValue();
		model.beginUpdate();
		try
		{
			newSpot = model.addSpotTo( new Spot( 0d, 0d, 0d, 1d, -1d ), firstFrame + 1 );
			model.addEdge( lFirstSpot, newSpot, 1 );
		}
		finally
		{
			model.endUpdate();
		}

		// The analyzer must have done something:
		assertTrue( analyzer.hasBeenCalled );

		// Check the track IDs: must be of size 1, and key to the track with
		// firstSpot and newSpot in it
		assertEquals( 1, analyzer.keys.size() );
		assertTrue( model.getTrackModel().trackSpots( analyzer.keys.iterator().next() ).contains( lFirstSpot ) );
		assertTrue( model.getTrackModel().trackSpots( analyzer.keys.iterator().next() ).contains( newSpot ) );

	}

	@Test
	public final void testModelChanged2()
	{

		// Copy old keys
		final HashSet< Integer > oldKeys = new HashSet<>( model.getTrackModel().trackIDs( true ) );

		// First analysis
		final TestTrackBranchingAnalyzer analyzer = new TestTrackBranchingAnalyzer( model );
		analyzer.process( oldKeys, model );

		// Reset analyzer
		analyzer.hasBeenCalled = false;
		analyzer.keys = null;

		// Prepare listener for model change
		final ModelChangeListener listener = new ModelChangeListener()
		{
			@Override
			public void modelChanged( final ModelChangeEvent event )
			{
				analyzer.process( event.getTrackUpdated(), model );
			}
		};
		model.addModelChangeListener( listener );
		// Reset analyzer
		analyzer.hasBeenCalled = false;
		analyzer.keys = null;

		/*
		 * A nasty change: we move a spot from its frame to the first frame, for
		 * a track with a split: it should turn it in a track with a merge.
		 */

		// Find a track with a split
		Integer splittingTrackID = null;
		for ( final Integer trackID : model.getTrackModel().trackIDs( true ) )
		{
			if ( model.getFeatureModel().getTrackFeature( trackID, TrackBranchingAnalyzer.NUMBER_SPLITS ) > 0 )
			{
				splittingTrackID = trackID;
				break;
			}
		}

		// Get the last spot in time
		final TreeSet< Spot > track = new TreeSet<>( Spot.frameComparator );
		track.addAll( model.getTrackModel().trackSpots( splittingTrackID ) );
		final Spot lastSpot = track.last();

		// Move the spot to the first frame. We do it with beginUpdate() /
		// endUpdate()
		model.beginUpdate();
		try
		{
			model.moveSpotFrom( lastSpot, lastSpot.getFeature( Spot.FRAME ).intValue(), 0 );
		}
		finally
		{
			model.endUpdate();
		}

		// The analyzer must have done something:
		assertTrue( analyzer.hasBeenCalled );

		// Check the track IDs: must be of size 1, and must be the track split
		assertEquals( 1, analyzer.keys.size() );
		assertNotNull( splittingTrackID );
		assertEquals( splittingTrackID.longValue(), analyzer.keys.iterator().next().longValue() );

		// Check that the features have been well calculated: it must now be a
		// merging track
		assertEquals( 1, model.getFeatureModel().getTrackFeature( splittingTrackID, TrackBranchingAnalyzer.NUMBER_SPLITS ).intValue() );
		assertEquals( 1, model.getFeatureModel().getTrackFeature( splittingTrackID, TrackBranchingAnalyzer.NUMBER_MERGES ).intValue() );
	}

	@Test
	public final void testModelChanged3()
	{

		// Copy old keys
		final HashSet< Integer > oldKeys = new HashSet<>( model.getTrackModel().trackIDs( true ) );

		// First analysis
		final TestTrackBranchingAnalyzer analyzer = new TestTrackBranchingAnalyzer( model );
		analyzer.process( oldKeys, model );

		// Reset analyzer
		analyzer.hasBeenCalled = false;
		analyzer.keys = null;

		// Prepare listener for model change
		final ModelChangeListener listener = new ModelChangeListener()
		{
			@Override
			public void modelChanged( final ModelChangeEvent event )
			{
				analyzer.process( event.getTrackUpdated(), model );
			}
		};
		model.addModelChangeListener( listener );
		// Reset analyzer
		analyzer.hasBeenCalled = false;
		analyzer.keys = null;

		/*
		 * A nasty change: we remove a spot from the frame where there is a
		 * split. Only the three generated tracks should get analyzed.
		 */

		// Remove the branching spot
		model.beginUpdate();
		try
		{
			model.removeSpot( split );
		}
		finally
		{
			model.endUpdate();
		}

		// The analyzer must have done something:
		assertTrue( analyzer.hasBeenCalled );

		// Check the track IDs: must be of size 3: the 3 tracks split
		assertEquals( 3, analyzer.keys.size() );
		for ( final Integer targetKey : analyzer.keys )
		{
			assertTrue( targetKey.equals( model.getTrackModel().trackIDOf( firstSpot ) ) || targetKey.equals( model.getTrackModel().trackIDOf( lastSpot1 ) ) || targetKey.equals( model.getTrackModel().trackIDOf( lastSpot2 ) ) );
		}

		// Check that the features have been well calculated: we must have 3
		// linear tracks
		for ( final Integer targetKey : analyzer.keys )
		{
			assertEquals( 0, model.getFeatureModel().getTrackFeature( targetKey, TrackBranchingAnalyzer.NUMBER_SPLITS ).intValue() );
			assertEquals( 0, model.getFeatureModel().getTrackFeature( targetKey, TrackBranchingAnalyzer.NUMBER_MERGES ).intValue() );
			assertEquals( 0, model.getFeatureModel().getTrackFeature( targetKey, TrackBranchingAnalyzer.NUMBER_COMPLEX ).intValue() );
		}
	}

	/**
	 * Subclass of {@link TrackIndexAnalyzer} to monitor method calls.
	 */
	private static final class TestTrackBranchingAnalyzer extends TrackBranchingAnalyzer
	{

		private boolean hasBeenCalled = false;

		private Collection< Integer > keys;

		/**
		 * @param model
		 */
		public TestTrackBranchingAnalyzer( final Model model )
		{
			super();
		}

		@Override
		public void process( final Collection< Integer > trackIDs, final Model model )
		{
			hasBeenCalled = true;
			keys = trackIDs;
			super.process( trackIDs, model );
		}

	}

}
