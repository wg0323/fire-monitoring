export type MapPointStatus = 'fire' | 'fault' | 'offline' | 'normal' | 'warning';

export interface MapPoint {
  id: string;
  name: string;
  lng: number;
  lat: number;
  status?: MapPointStatus;
  subtitle?: string;
  picUrl?: string | null;
  typeCode?: string | null;
  typeName?: string | null;
  alarmTime?: string | null;
  contact?: string | null;
  phone?: string | null;
}

export interface MapPick {
  lng: number;
  lat: number;
  address?: string;
}
