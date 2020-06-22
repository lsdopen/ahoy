export class Notification {
  text: string;
  viewed: boolean;
  error: boolean;

  constructor(text: string, error = false) {
    this.text = text;
    this.viewed = false;
    this.error = error;
  }
}
