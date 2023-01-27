/*
 * Copyright (c) 2019 Naman Dwivedi.
 *
 * Licensed under the GNU General Public License v3
 *
 * This is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 */
package com.naman14.timberx.ui.fragments

import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.naman14.timberx.R
import com.naman14.timberx.constants.Constants.ARTIST
import com.naman14.timberx.constants.Constants.SONG
import com.naman14.timberx.constants.Constants.SONG_ID
import com.naman14.timberx.databinding.FragmentLyricsBinding
import com.naman14.timberx.extensions.argument
import com.naman14.timberx.extensions.disposeOnDetach
import com.naman14.timberx.extensions.inflateWithBinding
import com.naman14.timberx.extensions.ioToMain
import com.naman14.timberx.extensions.subscribeForOutcome
import com.naman14.timberx.network.ONLINE_LYRICS_PREFERENCE
import com.naman14.timberx.network.Outcome
import com.naman14.timberx.network.api.LyricsRestService
import com.naman14.timberx.ui.fragments.base.BaseNowPlayingFragment
import com.naman14.timberx.util.AutoClearedValue
import com.naman14.timberx.util.LyricsExtractor
import com.naman14.timberx.util.MusicUtils
import org.koin.android.ext.android.inject
import timber.log.Timber
import java.io.File

class LyricsFragment : BaseNowPlayingFragment() {
    companion object {
        fun newInstance(songId: String, artist: String, title: String): LyricsFragment {
            return LyricsFragment().apply {
                arguments = Bundle().apply {
                    putString(SONG_ID, songId)
                    putString(ARTIST, artist)
                    putString(SONG, title)
                }
            }
        }
    }

    private lateinit var artistName: String
    private lateinit var songId: String
    lateinit var songTitle: String
    var binding by AutoClearedValue<FragmentLyricsBinding>(this)

    private val lyricsService by inject<LyricsRestService>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = inflater.inflateWithBinding(R.layout.fragment_lyrics, container)
        artistName = argument(ARTIST)
        songTitle = argument(SONG)
        songId = argument(SONG_ID)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        binding.songTitle = songTitle

        val songUri = MusicUtils.getSongUri(songId.toLong())
        val songPath = MusicUtils.getRealPathFromURI(requireContext(), songUri)
        val songFile = File(songPath)
        val lyrics = LyricsExtractor.getLyrics(songFile) ?: ""
        if (PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(ONLINE_LYRICS_PREFERENCE, false)) {
            lyricsService.getLyrics(artistName, songTitle)
                    .ioToMain()
                    .subscribeForOutcome { outcome ->
                        when (outcome) {
                            is Outcome.Success -> binding.lyrics = outcome.data
                            else -> {
                                binding.lyrics = lyrics
                            }
                        }
                    }
                    .disposeOnDetach(view)
        }
        else {
            binding.lyrics = lyrics
        }
    }
}
