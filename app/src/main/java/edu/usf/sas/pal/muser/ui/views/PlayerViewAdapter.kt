package edu.usf.sas.pal.muser.ui.views

import edu.usf.sas.pal.muser.ui.screens.nowplaying.PlayerView

abstract class PlayerViewAdapter : PlayerView {

    override fun setSeekProgress(progress: Int) {

    }

    override fun currentTimeVisibilityChanged(visible: Boolean) {

    }

    override fun currentTimeChanged(seconds: Long) {

    }

    override fun totalTimeChanged(seconds: Long) {

    }

    override fun queueChanged(queuePosition: Int, queueLength: Int) {

    }

    override fun playbackChanged(isPlaying: Boolean) {

    }

    override fun shuffleChanged(shuffleMode: Int) {

    }

    override fun repeatChanged(repeatMode: Int) {

    }

    override fun favoriteChanged(isFavorite: Boolean) {

    }

    override fun trackInfoChanged(song: edu.usf.sas.pal.muser.model.Song?) {

    }

    override fun showLyricsDialog() {

    }

    override fun showUpgradeDialog() {

    }

    override fun presentCreatePlaylistDialog(songs: List<edu.usf.sas.pal.muser.model.Song>) {

    }

    override fun presentSongInfoDialog(song: edu.usf.sas.pal.muser.model.Song) {

    }

    override fun onSongsAddedToPlaylist(playlist: edu.usf.sas.pal.muser.model.Playlist, numSongs: Int) {

    }

    override fun onSongsAddedToQueue(numSongs: Int) {

    }

    override fun presentTagEditorDialog(song: edu.usf.sas.pal.muser.model.Song) {

    }

    override fun presentDeleteDialog(songs: List<edu.usf.sas.pal.muser.model.Song>) {

    }

    override fun shareSong(song: edu.usf.sas.pal.muser.model.Song) {

    }

    override fun presentRingtonePermissionDialog() {

    }

    override fun showRingtoneSetMessage() {

    }
}