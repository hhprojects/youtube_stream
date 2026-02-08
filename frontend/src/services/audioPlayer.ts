import { Audio } from 'expo-av';

export type PlaybackStatus = {
  isPlaying: boolean;
  isBuffering: boolean;
  positionMillis: number;
  durationMillis: number;
  didJustFinish: boolean;
};

class AudioPlayer {
  private sound: Audio.Sound | null = null;
  private listeners: Array<(status: PlaybackStatus) => void> = [];
  private currentUrl: string | null = null;

  async load(url: string): Promise<void> {
    if (this.sound) {
      await this.unload();
    }

    try {
      const { sound } = await Audio.Sound.createAsync(
        { uri: url },
        {
          shouldPlay: false,
          progressUpdateIntervalMillis: 1000,
        },
        this.onPlaybackStatusUpdate
      );

      this.sound = sound;
      this.currentUrl = url;
    } catch (error) {
      console.error('Failed to load audio:', error);
      throw error;
    }
  }

  async play(): Promise<void> {
    if (!this.sound) {
      throw new Error('No audio loaded');
    }

    try {
      const status = await this.sound.getStatusAsync();
      if (status.isLoaded) {
        await this.sound.playAsync();
      }
    } catch (error) {
      console.error('Failed to play audio:', error);
      throw error;
    }
  }

  async pause(): Promise<void> {
    if (!this.sound) return;

    try {
      const status = await this.sound.getStatusAsync();
      if (status.isLoaded && status.isPlaying) {
        await this.sound.pauseAsync();
      }
    } catch (error) {
      console.error('Failed to pause audio:', error);
    }
  }

  async stop(): Promise<void> {
    if (!this.sound) return;

    try {
      const status = await this.sound.getStatusAsync();
      if (status.isLoaded) {
        await this.sound.stopAsync();
        await this.sound.setPositionAsync(0);
      }
    } catch (error) {
      console.error('Failed to stop audio:', error);
    }
  }

  async seek(positionMillis: number): Promise<void> {
    if (!this.sound) return;

    try {
      const status = await this.sound.getStatusAsync();
      if (status.isLoaded) {
        await this.sound.setPositionAsync(positionMillis);
      }
    } catch (error) {
      console.error('Failed to seek audio:', error);
    }
  }

  async unload(): Promise<void> {
    if (this.sound) {
      try {
        await this.sound.unloadAsync();
        this.sound = null;
        this.currentUrl = null;
      } catch (error) {
        console.error('Failed to unload audio:', error);
      }
    }
  }

  onPlaybackStatusUpdate = (status: any): void => {
    if (!status.isLoaded) return;

    const playbackStatus: PlaybackStatus = {
      isPlaying: status.isPlaying ?? false,
      isBuffering: status.isBuffering ?? false,
      positionMillis: status.positionMillis ?? 0,
      durationMillis: status.durationMillis ?? 0,
      didJustFinish: status.didJustFinish ?? false,
    };

    this.listeners.forEach(listener => listener(playbackStatus));
  };

  addListener(listener: (status: PlaybackStatus) => void): void {
    this.listeners.push(listener);
  }

  removeListener(listener: (status: PlaybackStatus) => void): void {
    this.listeners = this.listeners.filter(l => l !== listener);
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
  } catch (error) {
    console.error('Failed to setup audio:', error);
  }
};
