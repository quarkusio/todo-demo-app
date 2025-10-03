import {LitElement, html, css} from 'lit';
import '@vaadin/vaadin-lumo-styles/vaadin-iconset.js';
import '@vaadin/icon';
import '@vaadin/icons';
import '@vaadin/text-field';
import '@vaadin/message-list';
import '@vaadin/message-input';
import '@vaadin/horizontal-layout';
import '@vaadin/progress-bar';

export const WEBSOCKET_BASE = `${window.location.protocol === "https:" ? "wss:" : "ws:"}//${window.location.host}`;

class TodosDetail extends LitElement {
    static styles = css`
        .dialogContents { 
            display: flex; 
            gap: 1rem; 
            flex-direction: column; 
            min-height: 50vh;
            min-width: 60vw;
            justify-content: space-between;
            overflow: hidden;
        }
        .heading {
            background: var(--lumo-primary-color-50pct);
            padding: 8px;
            font-size: var(--lumo-font-size-xl);
            color: var(--lumo-success-contrast-color);
            border-width: 2px;
            border-style: solid;
            border-color: var(--lumo-contrast-5pct);
            border-radius: 10px;
        }
        .description {
            width: 100%;
        }
        .done-text { 
            text-decoration: line-through; 
        }
        .done-icon { 
            color: var(--lumo-success-color-50pct); 
        }
        .outstanding-icon { 
            color: var(--lumo-contrast-30pct); 
        }
        .sub { 
            color: var(--lumo-contrast-70pct); 
        }
        .badgeAndUrl { 
            display: flex; 
            justify-content: space-between; 
            align-items: center;
        }
        .badge { 
            background-color: var(--lumo-contrast-50pct); 
            color: white; 
            padding: 1px 4px; 
            text-align: right; 
            border-radius: 3px; 
        }
        .doWithAiButton {
            display: flex;
            justify-content: center;
            cursor: pointer;
            color: var(--lumo-success-text-color);
            font-size: var(--lumo-font-size-l);
        }
    
        .doWithAiButton :hover{
            font-size: var(--lumo-font-size-xl);
        }
    
        .aiMessages {
            display: flex;
            flex-direction: column;
            justify-content: end;
            flex: 1;
            max-height: 50vh;
        }
    
        .aiMessageBoard {
            flex: 1;
        }
        .aiMessageInput {
            display: flex;
            align-items: center;
            justify-content: space-between;
        }
        vaadin-message-input {
            flex: 1;
        }    
        .activityLog {
            color: var(--lumo-contrast-50pct);
        }
    `;
    
    static properties = {
        id: {type: Number},
        task: {type: String},
        description: {type: String},
        order: {type: Number},
        url: {type: String},
        done: {type: Boolean, reflect: true},
        _isInEditMode: {type: Boolean},
        _messageListItems: {type: Array},
        _isConnected:  {type: Boolean}
    };
  
    constructor() {
        super();
        this.id = -1;
        this.task = "";
        this.description = "";
        this.order = 0;
        this.url = null;
        this.done = false;
        this._isInEditMode = false;
        this._messageListItems = [];
        this._isConnected = false;
        this._webSocket = null;
    }
  
    connectedCallback() {
        super.connectedCallback();
        if(this.description){
            this._isInEditMode = false;
        }else{
            this._isInEditMode = true;
        }
    }
  
    disconnectedCallback() {
        this._cancelDoWithAI();
        super.disconnectedCallback();
    }
  
    render() {
        if(this.task){
            return html`<div class="dialogContents">
                            <div>
                                <div class="heading">
                                    <vaadin-icon icon="${this._icon()}" class="${this._iconClass()}"></vaadin-icon>
                                    <span class="${this._textClass()}">${this.task}</span>
                                </div>    
                                ${this._renderDescription()}    
                            </div>
                            ${this._renderCenter()}
                            <div class="badgeAndUrl">
                                <span class="badge">${this.id}</span>
                                ${this._renderUrl()}
                            </div>
                        </div>`;
        }
    }
    
    _renderDescription(){
        return html`
            <vaadin-text-field
                class="description"
                label="Description"
                placeholder="Enter a description for this task"
                .value=${this.description ?? ''}
                ?readonly=${!this._isInEditMode}
                @value-changed=${(e) => (this.description = e.detail.value)}
                @keydown=${this._maybeSaveOnEnter}
            >
                ${!this._isInEditMode
                    ? html`<vaadin-icon slot="suffix" icon="vaadin:pencil" style="cursor:pointer"
                       @click=${this._editDescription}></vaadin-icon>`
                  : html`<vaadin-icon slot="suffix" icon="vaadin:check" style="cursor:pointer"
                       @click=${this._saveDescription}></vaadin-icon>`}
            </vaadin-text-field>
        `;
    }
    
    _renderCenter(){
        if(this._isConnected){
            return this._renderAIMessageList();
        }else{
            return this._renderDoWithAIButton();
        }
    }
    
    _renderDoWithAIButton(){
        return html`<div class="doWithAiButton">
                            <vaadin-button theme="tertiary success" @click=${this._doWithAI}>
                                <vaadin-icon icon="vaadin:magic" slot="prefix"></vaadin-icon> Do with AI
                            </vaadin-button>
                    </div>`;
    }
    
    _maybeSaveOnEnter(e) {
        if (e.key === 'Enter') {
          e.preventDefault();
          this._saveDescription();
        }
    }

    _saveDescription(e){
        this._isInEditMode = false;
        this._updateTask();
    }
    
    _editDescription(e){
        this._isInEditMode = true;
    }
    
    _renderUrl(){
        if(this.url){
            return html`<vaadin-icon style="cursor: pointer; color: var(--lumo-primary-text-color);" icon="vaadin:external-link" @click=${this._openUrl}></vaadin-icon>`;
        }
    }
    
    _openUrl(e) {
        e.stopPropagation();
        if (this.url) window.open(this.url, '_blank', 'noopener');
    };
    
    _icon(){
        if(this.done){
            return "vaadin:check-square-o";
        }else {
            return "vaadin:thin-square";
        }
    }
    
    _iconClass(){
        if(this.done){
            return "done-icon";
        }else {
            return "outstanding-icon";
        }
    }
    
    _textClass(){
        if(this.done){
            return "done-text";
        }else {
            return "outstanding-text";
        }
    }
    
    _updateTask(){
        
        let updatedTask = {
            id: this.id,
            title: this.task,
            description: this.description,
            completed: this.done,
            order: this.order,
            url: this.url
        };
        
        const request = new Request('/api/' + this.id, {
                                            method: 'PATCH',
                                            body: JSON.stringify(updatedTask),
                                            headers: {
                                                'Content-Type': 'application/json'
                                            }
                                        });
        fetch(request);
    }
    
    _doWithAI(e){
        // TODO: Should this only be possible if there is a description ?
        this._webSocket = new WebSocket(WEBSOCKET_BASE + "/todo-agent/" + this.id);
        
        this._webSocket.addEventListener('open', () => {
            this._isConnected = true;
        });
    
        this._webSocket.addEventListener('message', (event) => {
            console.log(event.data);
            var agentMessage = JSON.parse(event.data);
            
            if(agentMessage.kind && agentMessage.kind === "activity_log"){
                this._messageListItems = [...this._messageListItems, this._createLogMessage(agentMessage.payload)];
            }else if(agentMessage.kind && agentMessage.kind === "agent_message"){
                this._messageListItems = [...this._messageListItems, this._createAgentMessage(agentMessage.payload)];
            }else{
                // TODO: Show in messages
                console.log("Unknown response " + event.data);
            }
        });
    
        this._webSocket.addEventListener('close', (e) => {
            this._webSocket = null;
            this._isConnected = false;
        });
        
        this._webSocket.addEventListener('error', (error) => {
            console.log(error);
            this._cancelDoWithAI();
        });
    }
    
    _cancelDoWithAI(){
        if(this._webSocket){
            this._sendToBackend("cancel");
            this._webSocket.close();
        }
        this._webSocket = null;
        this._isConnected = false;
        this._messageListItems = [];
    }
    
    _renderAIMessageList(){
        return html`<div class="aiMessages">
            <vaadin-message-list class="aiMessageBoard" .items="${this._messageListItems}" markdown></vaadin-message-list>
            <div class="aiMessageInput">
                <vaadin-message-input @submit="${this._handleChatSubmit}"></vaadin-message-input>
                <vaadin-button theme="primary error" @click="${this._cancelDoWithAI}">Cancel</vaadin-button>
            </div>
        </div>
        `;
    }
    
    _renderProgressBar(message) {
        return html`
            <div>
              <vaadin-horizontal-layout style="justify-content: space-between;">
                <label class="text-secondary" id="pblabel">${message}</label>
              </vaadin-horizontal-layout>

              <vaadin-progress-bar aria-labelledby="pblabel" indeterminate></vaadin-progress-bar>
            </div>
        `;
    }
    
    _createUserMessage(text){
        return {
            text,
            userName: 'User',
            userColorIndex: 1
        };
    }
    
    _createAgentMessage(text) {
        return {
            text,
            userName: 'Assistant',
            userColorIndex: 2
        };
    }
    
    _createLogMessage(text) {
        return {
            text,
            userName: 'Log',
            userColorIndex: 3,
            className: 'activityLog'
        };
    }
    
    _handleChatSubmit(e){
        const userInput = e.detail.value;
        this._messageListItems = [...this._messageListItems, this._createUserMessage(userInput)];
        this._sendToBackend("user_message", userInput);
        // TODO: Show progress
    }
    
    _sendToBackend(kind, message = ''){
        if(this._webSocket) {
            this._webSocket.send(this._createWsPayload(kind, message));
        }
    }
    
    _createWsPayload(kind, message = ''){
        let wspayload = {};
        if(message) {
            wspayload =  { 
                "kind": kind,
                "todoId": this.id,
                "payload": message
            };
        }else{
            wspayload = { 
                "kind": kind,
                "todoId": this.id
            };
        }
        
        return JSON.stringify(wspayload);
    }
    
}
customElements.define('todos-detail', TodosDetail);