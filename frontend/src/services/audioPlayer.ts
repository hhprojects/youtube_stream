import { Audio } from 'expo-audio';

export type PlaybackStatus = {
  isPlaying: boolean;
  isBuffering: boolean;
  positionMillis: number;
  durationMillis: number;
  didJustFinish: boolean;
};

class AudioPlayer {
  private player: Audio.Audio | null = null;
  private listeners: Array<(status: PlaybackStatus) => void> = [];
  private currentUrl: string | null = null;
  private playbackStatus: PlaybackStatus = {
    isPlaying: false,
    isBuffering: false,
    positionMillis: 0,
    durationMillis: 0,
    didJustFinish: false,
  };
  private playbackStatusUpdateInterval: NodeJS.Timeout | null = null;

  async load(url: string): Promise<void> {
    if (this.player) {
      await this.unload();
    }

    try {
      const player = new Audio.Audio();

      await player.loadAsync({ uri: url });

      // Set up status update listener
      player.setOnPlaybackStatusUpdate(this.onPlaybackStatusUpdate);

      this.player = player;
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
      const status = await this.player.getStatusAsync();
      if (status.isPlaying) {
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
      const status = await this.player.getStatusAsync();
      if (status.isPlaying || status.isLoaded) {
        await this.player.stopAsync();
        await this.player.setPositionAsync(0);
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
      await this.player.setPositionAsync(positionMillis);
      this.playbackStatus.positionMillis = positionMillis;
      this.notifyListeners();
    } catch (error) {
      console.error('Failed to seek audio:', error);
    }
  }

  async unload(): Promise<void> {
    if (this.player) {
      try {
        await this.player.unloadAsync();
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
    this.playbackStatusUpdateInterval = setInterval(async () => {
      if (this.player) {
        try {
          const status = await this.player.getStatusAsync();
          this.playbackStatus = {
            isPlaying: status.isPlaying,
            isBuffering: status.isBuffering || false,
            positionMillis: status.positionMillis || 0,
            durationMillis: status.durationMillis || 0,
            didJustFinish: status.didJustFinish || false,
          };

          if (status.didJustFinish) {
            this.notifyListeners();
          }
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

  onPlaybackStatusUpdate = (status: any): void => {
    if (!status) return;

    this.playbackStatus = {
      isPlaying: status.isPlaying ?? false,
      isBuffering: status.isBuffering ?? false,
      positionMillis: status.positionMillis ?? 0,
      durationMillis: status.durationMillis ?? 0,
      didJustFinish: status.didJustFinish ?? false,
    };

    this.notifyListeners();
  };

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
    await Audio.setAudioModeAsync({
      playsInSilentModeIOS: true,
      staysActiveInBackground: true,
      shouldDuckAndroid: true,
    });

    console.log('Audio mode configured successfully');
  } catch (error) {
    console.error('Failed to setup audio:', error);
  }
};
