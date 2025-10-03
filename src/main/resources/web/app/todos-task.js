import {LitElement, html, css} from 'lit';
import '@vaadin/icon';
import '@vaadin/vaadin-lumo-styles/vaadin-iconset.js';
import '@vaadin/icons';
import '@vaadin/dialog';
import '@vaadin/text-field';
import { dialogFooterRenderer, dialogRenderer } from '@vaadin/dialog/lit.js';
import './todos-detail.js';

class TodosTask extends LitElement {
    static styles = css`
        .item {
            display: flex;
            justify-content:space-between;
            font-size: 24px;
            font-weight: 300;
            width: 100%;
            gap: 20px;
        }
        .done-icon {
            color: var(--lumo-success-color-50pct);
            cursor: pointer;
            padding-left: 5px;
        }
        .outstanding-icon {
            color: var(--lumo-contrast-30pct);
            cursor: pointer;
            padding-left: 5px;
        }
        .done-text {
            text-decoration: line-through;
            color: var(--lumo-contrast-50pct);
        }
        .icon {
            cursor: pointer;
            padding-right: 5px;
        }
        .delete-icon {
            color: var(--lumo-error-color-50pct);
        }
        .edit-icon {
            font-size: initial;
            color: var(--lumo-primary-text-color);
        }
        .hide {
            visibility:hidden;
        }
        .taskInput {
            width: 100%;
            padding-right: 5px;
            padding-left: 5px;
        }
    `;
    
    static properties = {
        id: {type: Number},
        task: {type: String},
        description: {type: String},
        order: {type: Number},
        url: {type: String},
        done: {type: Boolean, reflect: true},
        _buttonClass: {type: String, attribute: false},
        _dialogOpened: {type: Boolean},
        _isInEditMode: {type: Boolean}
    };
  
    constructor() {
        super();
        this.id = -1;
        this.task = null;
        this.description = "";
        this.order = 0;
        this.url = null;
        this.done = false;
        this._buttonClass = "hide";
        this._dialogOpened = false;
        this._isInEditMode = false;
    }
  
    connectedCallback() {
        super.connectedCallback();
        this.addEventListener('mouseenter', this._handleMouseenter);
        this.addEventListener('mouseleave', this._handleMouseleave);
    }
  
    render() {
            return html`${this._renderDialog()}
            <div class="item">
                ${this._renderText()}
            </div>`;
    }
    
    _renderText(){
        if(this._isInEditMode){
            return html`<vaadin-text-field
                class="taskInput"
                placeholder="Enter the task title"
                .value=${this.task ?? ''}
                @value-changed=${(e) => (this.task = e.detail.value)}
                @keydown=${this._maybeSaveOnEnter}
            >
                <vaadin-icon title="Save" slot="suffix" icon="vaadin:check" style="cursor:pointer; color:var(--lumo-success-color);"
                       @click=${this._edit}></vaadin-icon>
            </vaadin-text-field>`;
        }else{
            return html`
                <div class="selectAndText">
                    <vaadin-icon icon="${this._icon()}" class="${this._iconClass()}" @click=${this._toggleSelect}></vaadin-icon>
                    <span class="${this._textClass()}" title="${this.description}" @click="${() => {
                                                                            this._dialogOpened = true;
                                                                        }}">${this.task}</span>
                </div>
                <div class="buttons">
                        ${this._renderEditButton()}
                        ${this._renderDeleteButton()}
                </div>
                `;
        }
    }
    
    _renderDeleteButton(){
        return html`<vaadin-icon icon="vaadin:close-small" title="Delete" class="${this._buttonClass} delete-icon" @click=${this._delete}></vaadin-icon>`;
    }
    
    _renderEditButton(){
        return html`<vaadin-icon icon="vaadin:pencil" title="Edit" class="${this._buttonClass} edit-icon" @click=${this._showEdit}></vaadin-icon>`;
    }
    
    _renderDialog() {
        return html`
            <vaadin-dialog
                resizable
                draggable
                .opened=${this._dialogOpened}
                @closed="${() => {
                    this._dialogOpened = false;
                }}"
                ${dialogRenderer(this._renderDialogContents, [this.done, this.task, this.description, this.url])}
                ${dialogFooterRenderer(this._renderDialogFooter, [this.done, this.task, this.description, this.url])}
            ></vaadin-dialog>`;
    }
    
    _renderDialogContents(){
        return html`<todos-detail 
                        id=${this.id} 
                        task="${this.task}" 
                        description="${this.description}" 
                        order=${this.order}
                        url="${this.url}" 
                        ?done=${this.done}>
                    </todos-detail>`;
    }
    
    _renderDialogFooter(){
        let t = "Mark as done";
        let c = "success-";
        
        if(this.done){
            t = "Mark as undone";
            c = "";
        }
        
        return html`
            <vaadin-button style="cursor: pointer; color: var(--lumo-error-text-color);" @click="${this._delete}">Delete</vaadin-button>
            <vaadin-button style="cursor: pointer; color: var(--lumo-${c}text-color);" @click="${this._toggleSelect}">${t}</vaadin-button>
        `;
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
    
    _handleMouseenter(){
       this._buttonClass = "icon";
    }
    
    _handleMouseleave(){
       this._buttonClass = "hide";
    }
    
    _toggleSelect(event){
        event = new CustomEvent('select', {detail: this.id, bubbles: true, composed: true});
        this.dispatchEvent(event);
    }
    
    _delete(event){
        event = new CustomEvent('delete', {detail: this.id, bubbles: true, composed: true});
        this.dispatchEvent(event);
        this._dialogOpened = false;
    }
    
    _showEdit(event){
        this._isInEditMode = true;
    }
    
    _edit(event){
        event = new CustomEvent('edit', {detail: {id:this.id, task:this.task}, bubbles: true, composed: true});
        this.dispatchEvent(event);
        this._isInEditMode = false;
    }
    
    _maybeSaveOnEnter(e) {
        if (e.key === 'Enter') {
          e.preventDefault();
          this._edit();
        }
    }
    
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
    
}
customElements.define('todos-task', TodosTask);