// Minimal type shims for CommonJS deps without bundled types.

declare module "lgtv2" {
  import type { EventEmitter } from "node:events";

  interface LGTVOptions {
    url?: string;
    timeout?: number;
    reconnect?: number | false;
    clientKey?: string;
    keyFile?: string;
    saveKey?: (key: string, cb: (err?: Error) => void) => void;
    wsconfig?: Record<string, unknown>;
  }

  interface PointerInputSocket {
    // lgtv2's SpecializedSocket: send("button", { name: "UP" }) → "type:button\nname:UP\n\n"
    send(type: string, payload?: Record<string, unknown>): void;
    close(): void;
  }

  interface LGTV extends EventEmitter {
    request(
      uri: string,
      payload?: Record<string, unknown> | null,
      cb?: (err: Error | null, res?: any) => void,
    ): void;
    subscribe(
      uri: string,
      cb?: (err: Error | null, res?: any) => void,
    ): void;
    getSocket(
      uri: string,
      cb: (err: Error | null, sock?: PointerInputSocket) => void,
    ): void;
    disconnect(): void;
    connect(host: string): void;
  }

  function lgtv(options?: LGTVOptions): LGTV;
  export = lgtv;
}

declare module "node-ssdp" {
  import type { EventEmitter } from "node:events";
  export interface SsdpHeaders {
    LOCATION?: string;
    SERVER?: string;
    ST?: string;
    USN?: string;
    [key: string]: string | undefined;
  }
  export class Client extends EventEmitter {
    constructor(options?: Record<string, unknown>);
    search(serviceType: string): void;
    stop(): void;
  }
  export class Server extends EventEmitter {
    constructor(options?: Record<string, unknown>);
  }
  const _default: { Client: typeof Client; Server: typeof Server };
  export default _default;
}
