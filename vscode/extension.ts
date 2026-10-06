import * as vscode from 'vscode';
import { execFileSync } from 'node:child_process';
import { LanguageClient } from 'vscode-languageclient/node';

let client: LanguageClient | null = null;

export async function activate(context: vscode.ExtensionContext) {
  const blls = vscode.workspace.getConfiguration('blls');
  if (!blls.get<boolean>('run', true)) return;

  if (blls.get<string>('path', '') === '') {
    const response = await vscode.window.showInformationMessage(
      'Не удалось найти blls',
      'Указать',
      'Использовать PATH',
    );

    switch (response) {
      case 'Указать':
        const uris = await vscode.window.showOpenDialog({
          canSelectFiles: true,
          canSelectFolders: false,
          canSelectMany: false,
          title: 'Выберите исполняемый файл blls',
        });
        if (uris) await blls.update('path', uris[0].path, true);
        else return;
        break;

      case undefined:
      case 'Использовать PATH':
        await blls.update('path', 'blls', true);
        break;
    }
  }

  await runLanguageServer();
}

export async function deactivate() {
  await stopLanguageServer();
}

async function runLanguageServer() {
  const blls = vscode.workspace.getConfiguration('blls');

  var path = blls.get<string>('path', 'blls');
  if (path === '') path = 'blls';

  const args: string[] = [];
  const z8 = blls.get<string>('z8', '');
  if (z8 !== '') args.push('--z8', z8);
  if (blls.get<boolean>('record', false)) args.push('--record');

  client = new LanguageClient(
    'blls',
    'blls',
    {
      command: path,
      args: args,
    },
    {
      documentSelector: [{ scheme: 'file', language: 'bl' }],
      outputChannel: vscode.window.createOutputChannel('blls', { log: true }),
      initializationOptions: {
        databaseVendor: blls.get<string>('databaseVendor', 'Postgres'),
        inlayHintBoolean: blls.get<boolean>('inlayHintBoolean', true),
        inlayHintNonObvious: blls.get<boolean>('inlayHintNonObvious', true),
        inlayHintImplicit: blls.get<boolean>('inlayHintImplicit', false),
        codeLensSql: blls.get<boolean>('codeLensSql', true),
      },
    },
  );
  await client.start();
}

async function stopLanguageServer() {
  const c = client;
  client = null;
  await c?.stop();
}
