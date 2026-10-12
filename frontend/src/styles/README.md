# Styles Architecture

Organizadas por componente e responsabilidade para melhor manutenção.

## Estrutura

### Core
- **variables.css** - Design tokens (cores, fontes, espaçamento)
- **base.css** - Reset global e estilos base
- **layout.css** - Grid principal do app com responsive breakpoints

### Componentes
- **topbar.css** - Barra superior (search, sync pill, avatar)
- **rail.css** - Navegação esquerda com ícones
- **sidebar.css** - Filtros e status sidebar
- **queue.css** - Lista de tickets e detail row
- **detail.css** - Painel direito principal
- **detail-aside.css** - Painel lateral com metadados

### Elementos
- **badges.css** - Status pills, priority, label chips
- **buttons.css** - Estilos de botão e variações
- **activity.css** - Feed de atividade e composer
- **utilities.css** - Toast, tweaks, dropdown, icons

## Breakpoints

```css
1024px  → Tablet: Grid reduzido
768px   → Mobile: Remove sidebar
480px   → Small mobile: Remove rail/queue
```

## Convenções

- Classes com BEM: `.component-element--modifier`
- Variáveis CSS para tudo (cores, espaçamento, shadows)
- Media queries inline dentro de cada arquivo
- Uma responsabilidade por arquivo
