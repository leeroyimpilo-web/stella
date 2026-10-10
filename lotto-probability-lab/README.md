# Lotto Lab

Mobile-first South African lottery number studio. The mathematics runs behind the interface: select a game, generate numbers, save selections and download an A4 PDF. No equations are displayed in the app.

## Run and verify

No runtime dependencies or build step. Requires a modern browser with ES modules, Web Crypto, BigInt, module Workers, Blob downloads and localStorage. Serve `dist/` over HTTP locally or HTTPS in production. Opening index.html directly from a file manager is unsupported.

```sh
npm test
npm start
```

Open http://localhost:8080. Node 20+ runs the tests; Python 3 supplies the local static server.

## Features

- Current South African Daily Lotto, Lotto, Lotto Plus 1, Lotto 5 Max, PowerBall and PowerBall Xtra.
- One-button generation for all six games; add-ons reuse base-game selections.
- Historical 6/58, 5/50+1/20 and Daily Lotto Plus formats, explicitly labelled historical.
- User-configurable unordered lotteries with up to two independent number pools, or one same-pool bonus ball.
- Uniform cryptographic random generation without duplicate tickets; include/exclude preferences; a greedy overlap-reduction heuristic; full combination wheels (maximum 200 lines).
- Exact single-ticket outcome probabilities, exact unique-line jackpot coverage, and independent repeated-draw probabilities.
- Actual portfolio union coverage: exhaustive enumeration for bounded problems, otherwise 60,000 independent simulations with 95% Wilson intervals. Calculations run in a cancellable worker. Jackpot queries always use the analytical count.
- Draw checking, local historical frequency inspection, and user-defined payout scenarios.
- Save up to 50 selections locally; copy numbers; dependency-free multi-page A4 PDF downloads.

## Verified rule snapshot: 10 October 2026

| Game | Format | Game portion | Required entries per line |
|---|---|---:|---:|
| Daily Lotto | 5/36 | R3 | R3 |
| Lotto | 6/52 | R5 | R5 |
| Lotto Plus 1 | 6/52 | R2.50 | R7.50 with Lotto |
| Lotto 5 Max | 6/52 | R2.50 | R10 with Lotto and Plus 1 |
| PowerBall | 5/50 + 1/16 | R10 | R10 |
| PowerBall Xtra | 5/50 + 1/16 | R5 | R15 with PowerBall |

Sources: [Nedbank](https://personal.nedbank.co.za/bank/digital-banking/needs/buy/lottery-faqs.html) and [Standard Bank](https://www.standardbank.co.za/southafrica/personal/products-and-services/ways-to-bank/help-centre/play-lotto). The operator homepage returned HTTP 403 during research. Bank/retailer service fees are excluded. Current rules must be rechecked before changing presets. Legacy prices are historical scenarios, not current offers.

## Important scope

This is an independent calculator, not a ticket sales service or a predictor. It has no live draw/jackpot feed. It supports the six named SA draw games and configurable unordered lottery structures, not every game worldwide. Raffles, sports pools, instant scratch games and ordered digits require different models and are not supported. Match categories include losing outcomes and are not advertised as official prize divisions.

Unique jackpot probabilities are exact counts; displayed decimal values are rounded floating-point approximations. Spread mode is a heuristic that reduces overlap, without an optimality or win-rate guarantee. A wheel's complete coverage is conditional on the winning main numbers lying in the chosen pool; its cost grows with its line count. History inspection is descriptive and never changes selection weights.

For a single add-on selection, cost includes prerequisite games but the odds and payout scenario only cover the selected draw. All-game PDF costs sum game portions, avoiding double counting the prerequisites. Local data is not synced or backed up; clearing browser data removes saved selections. PDF downloads provide an independent copy.

See `RESEARCH.md` for the implementation rationale. The main app intentionally contains no equations.

## Validation

`npm test` checks exact combinatorial counts, independently enumerated distributions, constraints, wheel coverage, duplicate handling, draw checking, simulation boundaries and dependent portfolio unions. PDF export has also been rendered and inspected as an A4 report. Browser visual QA was unavailable in the managed build environment; mobile CSS and app assets were checked statically. No browser compatibility claim is made beyond the stated required APIs.
