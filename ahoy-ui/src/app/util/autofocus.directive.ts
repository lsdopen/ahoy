import {AfterViewInit, Directive, ElementRef} from '@angular/core';

@Directive({
  selector: '[appAutofocus]'
})
export class AutofocusDirective implements AfterViewInit {

  constructor(private host: ElementRef) {
  }

  ngAfterViewInit() {
    this.host.nativeElement.focus();
  }
}
