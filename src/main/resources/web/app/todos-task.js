import {LitElement, html, css} from 'lit';
import '@vaadin/icon';
import '@vaadin/vaadin-lumo-styles/vaadin-iconset.js';
import '@vaadin/icons';
import '@vaadin/dialog';
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
        .delete-icon {
            color: var(--lumo-error-color);
            cursor: pointer;
            padding-right: 5px;
        }
        .hide {
            visibility:hidden;
        }
    `;
    
    static properties = {
        id: {type: Number},
        task: {type: String},
        description: {type: String},
        order: {type: Number},
        url: {type: String},
        done: {type: Boolean, reflect: true},
        _deleteButtonClass: {type: String, attribute: false},
        _dialogOpened: {type: Boolean}
    };
  
    constructor() {
        super();
        this.id = -1;
        this.task = "";
        this.description = "";
        this.order = 0;
        this.url = null;
        this.done = false;
        this._deleteButtonClass = "hide";
        this._dialogOpened = false;
    }
  
    connectedCallback() {
        super.connectedCallback();
        this.addEventListener('mouseenter', this._handleMouseenter);
        this.addEventListener('mouseleave', this._handleMouseleave);
    }
  
    render() {
        if(this.task){
            return html`${this._renderDialog()}<span class="item">
                <span><vaadin-icon icon="${this._icon()}" class="${this._iconClass()}" @click=${this._toggleSelect}></vaadin-icon> 
                <span class="${this._textClass()}" title="${this.description}" @click="${() => {
                                                                            this._dialogOpened = true;
                                                                        }}">${this.task}</span></span>
                ${this._renderDeleteButton()}
            </span>`;
        }
    }
    
    _renderDeleteButton(){
        return html`<vaadin-icon icon="vaadin:close-small" class="${this._deleteButtonClass}" @click=${this._delete}></vaadin-icon>`;
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
       this._deleteButtonClass = "delete-icon";
    }
    
    _handleMouseleave(){
       this._deleteButtonClass = "hide";
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