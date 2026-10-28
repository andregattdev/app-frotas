# FrotaApp

This project was generated using [Angular CLI](https://github.com/angular/angular-cli) version 22.1.4.

## Development server

To start a local development server, run:

```bash
ng serve
```

Once the server is running, open your browser and navigate to `http://localhost:4200/`. The application will automatically reload whenever you modify any of the source files.

## Access from devices on the same Wi-Fi

Run the backend and frontend on the same computer. Both servers listen on the network interfaces; the frontend automatically sends API requests to the hostname used to open the app.

1. Start PostgreSQL and the backend using the usual local database and authentication environment variables.
2. Start the frontend with `npm start`.
3. On the computer running the servers, run `ipconfig` and find its IPv4 address on the Wi-Fi adapter (for example, `192.168.1.25`).
4. On another device connected to that same Wi-Fi, open `http://<IPv4-address>:4200` (for example, `http://192.168.1.25:4200`).

If Windows Firewall prompts, allow the frontend (port `4200`) and backend (port `8080`) on **private networks only**. Keep PostgreSQL bound to the host computer; it does not need to be exposed to the Wi-Fi network.

### Optional local HTTPS

The development HTTP setup above is sufficient for photo capture and its server-recorded date/time watermark; no location permission or HTTPS setup is required. For encrypted traffic on the local network, stop both servers, install `mkcert` for Windows, then run `.\setup-local-https.ps1` from the repository root. The script creates an untracked local certificate for `localhost` and the computer's current LAN IPv4 address. Do not share or commit the generated private key.

Install only `rootCA.pem` from the directory printed by `mkcert -CAROOT` on each phone as a trusted certificate authority. Never copy or install `rootCA-key.pem`. On iOS, also enable full trust for the installed root certificate. The browser must trust this CA before opening the app.

Start the backend from `Backend\api-frota`, keeping the usual database and authentication environment variables set:

```powershell
$env:SPRING_PROFILES_ACTIVE = 'https'
mvn spring-boot:run
```

Start the frontend from `Frontend\frota-app`:

```powershell
npm run start:https
```

Open `https://<IPv4-address>:4200` on the phone. If the LAN IPv4 address changes, regenerate the certificate and update the matching `allowedHosts` entries in `angular.json`. The application calls the backend over HTTPS on port `8080`.

GPS is not requested or required when recording a delivery, collection, or occurrence. The server adds the registration date and time to the saved image watermark. Historical records that already include GPS coordinates continue to show them in the administrator's history and CSV export.

## Driver stop photos

Drivers must attach a JPEG or PNG photo (up to 8 MB) when confirming a delivery/collection or reporting an occurrence. Photos are stored on the backend computer, outside PostgreSQL, in `Backend/api-frota/uploads` when the backend is started from its project directory. Set the backend environment variable `UPLOAD_DIR` to an absolute path on persistent storage when using another working directory or deployment. Back up this folder together with the database; the database stores only each photo's generated filename. Existing historical records without photos remain available and are marked as such.

## Authentication and roles

The API uses stateless JWT authentication. Set `JWT_SECRET` to a private random value of at least 32 UTF-8 bytes before starting the backend. Never commit this value or use a development secret in production.

Create the first administrator by setting all three backend environment variables before startup:

- `BOOTSTRAP_ADMIN_USERNAME`
- `BOOTSTRAP_ADMIN_EMAIL`
- `BOOTSTRAP_ADMIN_PASSWORD` (at least 12 characters)

The application creates the administrator only when that username does not already exist. The public registration endpoint, if used, grants only the basic `USER` role; administrator accounts are provisioned through the bootstrap variables. Administrators create driver accounts from the Motoristas page and assign eligible drivers to a vehicle in its route page. Drivers can view only their active assignments from **Minhas rotas**.

Use HTTPS in deployments so login credentials and bearer tokens are protected in transit.

## Code scaffolding

Angular CLI includes powerful code scaffolding tools. To generate a new component, run:

```bash
ng generate component component-name
```

For a complete list of available schematics (such as `components`, `directives`, or `pipes`), run:

```bash
ng generate --help
```

## Building

To build the project run:

```bash
ng build
```

This will compile your project and store the build artifacts in the `dist/` directory. By default, the production build optimizes your application for performance and speed.

## Running unit tests

To execute unit tests with the [Vitest](https://vitest.dev/) test runner, use the following command:

```bash
ng test
```

## Running end-to-end tests

For end-to-end (e2e) testing, run:

```bash
ng e2e
```

Angular CLI does not come with an end-to-end testing framework by default. You can choose one that suits your needs.

## Additional Resources

For more information on using the Angular CLI, including detailed command references, visit the [Angular CLI Overview and Command Reference](https://angular.dev/tools/cli) page.
