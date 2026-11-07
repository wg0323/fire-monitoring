export type MerchantTypeSymbol =
  | 'food'
  | 'shop'
  | 'hotel'
  | 'school'
  | 'hospital'
  | 'elderly'
  | 'other';

/** 字典 dict_merchant_type 的类型代码；未知代码统一落到“其他”。 */
const TYPE_SYMBOLS: Record<string, MerchantTypeSymbol> = {
  '001': 'food',
  '002': 'shop',
  '003': 'hotel',
  '004': 'other',
  '005': 'school',
  '006': 'hospital',
  '007': 'elderly',
};

export function merchantTypeSymbol(code?: string | null): MerchantTypeSymbol {
  return (code && TYPE_SYMBOLS[code]) || 'other';
}

/** 供高德 Marker content 使用的图标片段，symbol 定义在 FmMap 的隐藏 sprite 中。 */
export function merchantTypeIconHtml(code?: string | null): string {
  return '<svg class="fm-map-marker__icon" viewBox="0 0 24 24" aria-hidden="true">'
    + `<use href="#fm-type-${merchantTypeSymbol(code)}"></use></svg>`;
}
