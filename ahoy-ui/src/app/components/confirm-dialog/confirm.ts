export class Confirmation {
  title = 'Confirm';
  message: string;
  requiresInput = false;
  input: string;
  verify = false;
  verifyText: string;
  enteredVerifyText: string;

  constructor(message: string) {
    this.message = message;
  }
}
