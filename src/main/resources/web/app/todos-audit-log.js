import {LitElement, html, css} from 'lit';
import '@vaadin/grid';
import { columnBodyRenderer } from '@vaadin/grid/lit.js';

class TodosAuditLog extends LitElement {
    
    static webSocket;
    static serverUri;
    
    static styles = css`
    `;
    
    static properties = {
        _entries: {type: Array, state: true}
    };
    
    constructor() {
        super();
        this._entries = [];
        if (!TodosAuditLog.logWebSocket) {
          if (window.location.protocol === "https:") {
            TodosAuditLog.serverUri = "wss:";
          } else {
            TodosAuditLog.serverUri = "ws:";
          }
          TodosAuditLog.serverUri += "//" + window.location.host + "/audit";
          TodosAuditLog.connect();
      }
      this._eventAuditEntry = (event) => this._receiveAuditEntry(event.detail);
    }
    
    connectedCallback() {
        super.connectedCallback();
        document.addEventListener('eventAuditEntryEvent', this._eventAuditEntry, false);
    }

    disconnectedCallback() {
        document.removeEventListener('eventAuditEntryEvent', this._eventAuditEntry, false);
        super.disconnectedCallback();
    }

    render() {
        return html`<vaadin-grid .items="${this._entries}">
                        <vaadin-grid-column header="Action" ${columnBodyRenderer(this._typeRenderer, [])}></vaadin-grid-column>
                        <vaadin-grid-column path="todo.id"></vaadin-grid-column>
                        <vaadin-grid-column path="todo.title"></vaadin-grid-column>
                        <vaadin-grid-column path="todo.completed"></vaadin-grid-column>
                    </vaadin-grid>`;
    }
  
    _typeRenderer(entry) {
        return html`${this._formatTodoType(entry.type)}`;
    }
  
    _formatTodoType(str) {
        return str.replace(/^TODO_(.*)$/, function(match, p1) {
            return p1.toLowerCase();
        });
    }
  
    _receiveAuditEntry(entry) {
        this._entries = [entry, ...this._entries];
    }
  
    static connect() {
        TodosAuditLog.webSocket = new WebSocket(TodosAuditLog.serverUri);
        TodosAuditLog.webSocket.onmessage = function (event) {
            var auditentry = JSON.parse(event.data);
            const eventAuditEntryEvent = new CustomEvent('eventAuditEntryEvent', {detail: auditentry});
            document.dispatchEvent(eventAuditEntryEvent);
        }
        TodosAuditLog.webSocket.onclose = function (event) {
            setTimeout(function () {
                TodosAuditLog.connect();
            }, 100);
        };
    }
  
}
customElements.define('todos-audit-log', TodosAuditLog);
