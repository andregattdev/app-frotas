import { Component } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { ActivatedRoute } from '@angular/router';
import { RouterLink } from '@angular/router';

@Component({
  imports: [MatIconModule, RouterLink],
  selector: 'app-acao-veiculo',
  styleUrl: './acao-veiculo.css',
  templateUrl: './acao-veiculo.html',
})
export class AcaoVeiculo {
  titulo: string;
  descricao: string;
  veiculoId: string | null;

  constructor(private route: ActivatedRoute) {
    this.titulo = this.route.snapshot.data['titulo'];
    this.descricao = this.route.snapshot.data['descricao'];
    this.veiculoId = this.route.snapshot.queryParamMap.get('veiculo');

    if (this.veiculoId && this.titulo === 'Documentação dos veículos') {
      this.titulo = `Documentação do veículo #${this.veiculoId}`;
      this.descricao = 'Aqui ficarão os documentos deste veículo da frota.';
    }

    if (this.veiculoId && this.titulo === 'Manutenção de veículos') {
      this.titulo = `Manutenção do veículo #${this.veiculoId}`;
      this.descricao = 'Aqui ficará o controle de manutenção deste veículo.';
    }
  }
}
