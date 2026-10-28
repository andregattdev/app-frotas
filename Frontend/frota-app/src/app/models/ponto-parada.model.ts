export type TipoParada = 'ENTREGA' | 'RETIRADA';

export interface PontoParada {
  id: number;
  descricao: string;
  nomeEstabelecimento: string | null;
  endereco: string | null;
  tipo: TipoParada;
  latitude: number;
  longitude: number;
  ordem: number;
}

export type PontoParadaRequest = Omit<PontoParada, 'id' | 'ordem'>;