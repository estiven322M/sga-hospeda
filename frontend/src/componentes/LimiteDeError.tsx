import { Component, type ErrorInfo, type ReactNode } from 'react';

interface LimiteDeErrorProps {
  /** Lo que se ve si algo de adentro falla al pintarse o al cargarse. */
  alternativa: ReactNode;
  children: ReactNode;
}

/**
 * Atrapa un fallo al pintar (o al descargar un módulo con lazy) y muestra una alternativa en lugar de romper toda la página. Los
 * límites de error solo se pueden escribir como clase: React no tiene todavía un equivalente con hooks. Se usa con el mapa:
 * si Leaflet no se descarga o falla, se ve la dirección con las coordenadas y la página sigue funcionando.
 */
export class LimiteDeError extends Component<LimiteDeErrorProps, { fallo: boolean }> {
  state = { fallo: false };

  static getDerivedStateFromError(): { fallo: boolean } {
    return { fallo: true };
  }

  componentDidCatch(error: Error, info: ErrorInfo): void {
    console.warn('Un componente falló y se muestra su alternativa:', error.message, info.componentStack);
  }

  render(): ReactNode {
    return this.state.fallo ? this.props.alternativa : this.props.children;
  }
}
