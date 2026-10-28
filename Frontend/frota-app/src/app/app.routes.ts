import { Routes } from '@angular/router';
import { Dashboard } from './components/dashboard/dashboard';
import { ListaChecklist } from './components/lista-checklist/lista-checklist';
import { ListaEnderecos } from './components/lista-enderecos/lista-enderecos';
import { ListaMotoristas } from './components/lista-motoristas/lista-motoristas';
import { ListaVeiculos } from './components/lista-veiculos/lista-veiculos';
import { AcaoVeiculo } from './components/acao-veiculo/acao-veiculo';
import { ManutencaoVeiculo } from './components/manutencao-veiculo/manutencao-veiculo';
import { RotaVeiculo } from './components/rota-veiculo/rota-veiculo';
import { Login } from './components/login/login';
import { MinhasRotas } from './components/minhas-rotas/minhas-rotas';
import { RegistrosParadas } from './components/registros-paradas/registros-paradas';
import { RotasDiarias } from './components/rotas-diarias/rotas-diarias';
import { MinhaConta } from './components/minha-conta/minha-conta';
import { AssistenteFrota } from './components/assistente-frota/assistente-frota';
import { roleGuard } from './guards/role.guard';


export const routes: Routes = [
  {
    path: 'login',
    component: Login
  },
  {
    path: 'assistente-frota',
    component: AssistenteFrota,
    canActivate: [roleGuard],
    data: { roles: ['ADMIN', 'SUPER_ADMIN', 'ASSISTENTE_FROTA'] }
  },
  {
    path: 'minhas-rotas',
    component: MinhasRotas,
    canActivate: [roleGuard],
    data: { roles: ['MOTORISTA'] }
  },
  {
    path: 'minha-conta',
    component: MinhaConta,
    canActivate: [roleGuard],
    data: { roles: ['MOTORISTA', 'ASSISTENTE_FROTA'] }
  },
  {
    path: 'registros-paradas',
    component: RegistrosParadas,
    canActivate: [roleGuard],
    data: { roles: ['ADMIN', 'SUPER_ADMIN'] }
  },
  {
    path: 'rotas-diarias',
    component: RotasDiarias,
    canActivate: [roleGuard],
    data: { roles: ['ADMIN', 'SUPER_ADMIN'] }
  },
  {
    path: '',
    component: Dashboard,
    canActivate: [roleGuard],
    data: { roles: ['ADMIN', 'SUPER_ADMIN'] }
  },
  {
    path: 'dashboard',
    component: Dashboard,
    canActivate: [roleGuard],
    data: { roles: ['ADMIN', 'SUPER_ADMIN'] }
  },
  {
    path: 'veiculos',
    component: ListaVeiculos,
    canActivate: [roleGuard],
    data: { roles: ['ADMIN', 'SUPER_ADMIN', 'ASSISTENTE_FROTA'] }
  },
  {
    path: 'veiculos/cadastrados',
    component: AcaoVeiculo,
    canActivate: [roleGuard],
    data: {
      roles: ['ADMIN', 'SUPER_ADMIN'],
      titulo: 'Veículos cadastrados',
      descricao: 'Aqui ficará a lista dos veículos cadastrados.'
    }
  },
  {
    path: 'veiculos/cadastrar',
    component: AcaoVeiculo,
    canActivate: [roleGuard],
    data: {
      roles: ['ADMIN', 'SUPER_ADMIN'],
      titulo: 'Cadastrar novo veículo',
      descricao: 'Aqui ficará o formulário para cadastrar um veículo.'
    }
  },
  {
    path: 'veiculos/manutencao',
    component: ManutencaoVeiculo,
    canActivate: [roleGuard],
    data: { roles: ['ADMIN', 'SUPER_ADMIN'] }
  },
  {
    path: 'veiculos/rotas',
    component: RotaVeiculo,
    canActivate: [roleGuard],
    data: { roles: ['ADMIN', 'SUPER_ADMIN'] }
  },
  {
    path: 'veiculos/documentacao',
    component: AcaoVeiculo,
    canActivate: [roleGuard],
    data: {
      roles: ['ADMIN', 'SUPER_ADMIN'],
      titulo: 'Documentação dos veículos',
      descricao: 'Aqui ficarão os documentos dos veículos da frota.'
    }
  },
  {
    path: 'motoristas',
    component: ListaMotoristas,
    canActivate: [roleGuard],
    data: { roles: ['ADMIN', 'SUPER_ADMIN'] }
  },
  {
    path: 'enderecos',
    component: ListaEnderecos,
    canActivate: [roleGuard],
    data: { roles: ['ADMIN', 'SUPER_ADMIN'] }
  },
  {
    path: 'oficinas',
    component: ListaEnderecos,
    canActivate: [roleGuard],
    data: { roles: ['ADMIN', 'SUPER_ADMIN', 'ASSISTENTE_FROTA'] }
  },
  {
    path: 'checklist',
    component: ListaChecklist,
    canActivate: [roleGuard],
    data: { roles: ['ADMIN', 'SUPER_ADMIN', 'MOTORISTA', 'ASSISTENTE_FROTA'] }
  },
  { path: '**', redirectTo: 'login' }
];
