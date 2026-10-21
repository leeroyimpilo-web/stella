# LeadRadar USA — Sprint 1 workstream

This isolated feature branch is reserved for the LeadRadar USA Next.js application. The existing Stella projects on `main` are intentionally untouched.

Sprint 1 application sources are packaged separately as `leadradar-usa-sprint1.zip` in the ChatGPT delivery. **The source files have not yet been committed to this branch.**

To import, unpack the archive at this repository root so that it creates a `leadradar-usa/` folder, and commit that folder. Move `leadradar-usa/render.yaml` to repository root before deploying via a Render Blueprint (its `rootDir` is already `leadradar-usa`). Configure `DATABASE_URL` from the dedicated Neon project `leadradar-usa` and a strong `ADMIN_PASSWORD` as Render secrets. Do not commit `.env` files.

The Neon project and initial schema have already been created. Craigslist automated scanning is disabled; only authorized/manual lead intake is implemented.
