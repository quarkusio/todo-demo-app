import {LitElement, css, html} from 'lit';

class TodosHeader extends LitElement {
  static styles = css`
        :host {
            display: flex;
            justify-content: center;
            font-size: 100px;
            line-height: 100px;
            height: 100px;
            font-weight: 100;
            color: rgba(175, 47, 47, 0.15);
            padding-top: 20px;
            padding-bottom: 20px;
        }
        
        .title {
            align-self: baseline;
            padding-left: 20px;
        }
        .logo {
            align-self: baseline;
            width: 64px;
            height: 64px;
        }
    `;
  render() {
    return html`<img class="logo" src="static/quarkus_icon.png"> <span class="title">todos</span>`;
  }
}
customElements.define('todos-header', TodosHeader);