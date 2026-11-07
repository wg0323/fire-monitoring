export interface AmapLngLat {
  lng: number;
  lat: number;
}

export interface AmapMap {
  add(overlays: unknown | unknown[]): void;
  remove(overlays: unknown | unknown[]): void;
  setFitView(overlays?: unknown[], immediately?: boolean, avoid?: number[], maxZoom?: number): void;
  setZoomAndCenter(zoom: number, center: [number, number] | AmapLngLat): void;
  on(event: string, handler: (event: AmapMapEvent) => void): void;
  off(event: string, handler: (event: AmapMapEvent) => void): void;
  resize(): void;
  destroy(): void;
}

export interface AmapMapEvent {
  lnglat: AmapLngLat;
}

export interface AmapMarker {
  on(event: string, handler: () => void): void;
  getPosition(): AmapLngLat;
}

export interface AmapInfoWindow {
  setContent(content: string): void;
  open(map: AmapMap, position: AmapLngLat): void;
  close(): void;
}

export interface AmapGeocodeResult {
  regeocode?: {
    formattedAddress?: string;
  };
}

export interface AmapGeocoder {
  getAddress(position: [number, number], callback: (status: string, result: AmapGeocodeResult) => void): void;
}

export interface AmapNamespace {
  Map: new (container: HTMLElement | string, options?: Record<string, unknown>) => AmapMap;
  Marker: new (options: Record<string, unknown>) => AmapMarker;
  InfoWindow: new (options?: Record<string, unknown>) => AmapInfoWindow;
  Geocoder: new (options?: Record<string, unknown>) => AmapGeocoder;
  Pixel: new (x: number, y: number) => unknown;
}

declare global {
  interface Window {
    _AMapSecurityConfig?: {
      securityJsCode?: string;
    };
    AMap?: AmapNamespace;
  }
}
