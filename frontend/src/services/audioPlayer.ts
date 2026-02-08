import { createAudioPlayer, setAudioModeAsync, AudioPlayer as ExpoAudioPlayer } from 'expo-audio';

export type PlaybackStatus = {
  isPlaying: boolean;
  isBuffering: boolean;
  positionMillis: number;
  durationMillis: number;
  didJustFinish: boolean;
};

class AudioPlayer {
  private player: ExpoAudioPlayer | null = null;
  private listeners: Array<(status: PlaybackStatus) => void> = [];
  private currentUrl: string | null = null;
  private playbackStatus: PlaybackStatus = {
    isPlaying: false,
    isBuffering: false,
    positionMillis: 0,
    durationMillis: 0,
    didJustFinish: false,
  };
  private statusUpdateInterval: NodeJS.Timeout | null = null;

  async load(url: string): Promise<void> {
    if (this.player) {
      await this.unload();
    }

    try {
      // Create audio player with source
      this.player = createAudioPlayer(url, {
        updateInterval: 1000,
      });

      this.currentUrl = url;

      // Start polling for playback status updates
      this.startPlaybackStatusPolling();
    } catch (error) {
      console.error('Failed to load audio:', error);
      throw error;
    }
  }

  async play(): Promise<void> {
    if (!this.player) {
      throw new Error('No audio loaded');
    }

    try {
      await this.player.playAsync();
      this.playbackStatus.isPlaying = true;
      this.notifyListeners();
    } catch (error) {
      console.error('Failed to play audio:', error);
      throw error;
    }
  }

  async pause(): Promise<void> {
    if (!this.player) return;

    try {
      const status = this.player.getStatus();
      if (status.playing) {
        await this.player.pauseAsync();
        this.playbackStatus.isPlaying = false;
        this.notifyListeners();
      }
    } catch (error) {
      console.error('Failed to pause audio:', error);
    }
  }

  async stop(): Promise<void> {
    if (!this.player) return;

    try {
      const status = this.player.getStatus();
      if (status.playing) {
        await this.player.stopAsync();
        this.playbackStatus.isPlaying = false;
        this.playbackStatus.positionMillis = 0;
        this.notifyListeners();
      }
    } catch (error) {
      console.error('Failed to stop audio:', error);
    }
  }

  async seek(positionMillis: number): Promise<void> {
    if (!this.player) return;

    try {
      // expo-audio uses seconds, not milliseconds
      const positionSeconds = positionMillis / 1000;
      await this.player.seekTo(positionSeconds);
      this.playbackStatus.positionMillis = positionMillis;
      this.notifyListeners();
    } catch (error) {
      console.error('Failed to seek audio:', error);
    }
  }

  async unload(): Promise<void> {
    if (this.player) {
      try {
        await this.player.stopAsync();
        this.player.release();
        this.player = null;
        this.currentUrl = null;
        this.stopPlaybackStatusPolling();
      } catch (error) {
        console.error('Failed to unload audio:', error);
      }
    }
  }

  private startPlaybackStatusPolling() {
    this.stopPlaybackStatusPolling();
    this.playbackStatusUpdateInterval = setInterval(() => {
      if (this.player) {
        try {
          const status = this.player.getStatus();

          this.playbackStatus = {
            isPlaying: status.playing,
            isBuffering: status.buffering || false,
            positionMillis: status.currentTime * 1000, // Convert seconds to milliseconds
            durationMillis: status.duration ? status.duration * 1000 : 0, // Convert seconds to milliseconds
            didJustFinish: false, // expo-audio doesn't have didJustFinish
          };

          this.notifyListeners();
        } catch (error) {
          console.error('Error polling playback status:', error);
        }
      }
    }, 1000);
  }

  private stopPlaybackStatusPolling() {
    if (this.playbackStatusUpdateInterval) {
      clearInterval(this.playbackStatusUpdateInterval);
      this.playbackStatusUpdateInterval = null;
    }
  }

  addListener(listener: (status: PlaybackStatus) => void): void {
    this.listeners.push(listener);
  }

  removeListener(listener: (status: PlaybackStatus) => void): void {
    this.listeners = this.listeners.filter(l => l !== listener);
  }

  private notifyListeners() {
    this.listeners.forEach(listener => listener({ ...this.playbackStatus }));
  }

  getCurrentUrl(): string | null {
    return this.currentUrl;
  }
}

export const audioPlayer = new AudioPlayer();

// Setup audio mode
export const setupAudio = async (): Promise<void> => {
  try {
    await setAudioModeAsync({
      playsInSilentModeIOS: true,
      staysActiveInBackground: true,
      shouldDuckAndroid: true,
    });

    console.log('Audio mode configured successfully');
  } catch (error) {
    console.error('Failed to setup audio:', error);
  }
};
