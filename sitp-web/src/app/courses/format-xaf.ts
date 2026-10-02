
export function formatXaf(price: number): string {
  const digits = Number.isInteger(price) ? price.toFixed(0) : price.toFixed(2);

  const [whole, tail] = digits.split('.');
  const grouped = whole.replace(/\B(?=(\d{3})+(?!\d))/g, ' ');

  return tail === undefined ? `${grouped} FCFA` : `${grouped}.${tail} FCFA`;
}
