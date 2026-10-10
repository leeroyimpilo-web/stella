# Probability engine research and audit notes

Reviewed 10 October 2026. These are developer notes, not an in-app formula lesson.

## What research can establish

There is no credible justification for claiming that reviewing every historical mathematician produces a winning-number formula. The useful lineage is finite counting, conditional probability, expectations and statistical estimation. Pascal and Fermat's correspondence is a historical foundation for counting uncertain outcomes, not a method to foresee lottery draws. Grinstead and Snell's textbook supplies the modern framework for independence, combinations and expectations. Siegrist directly develops lottery matching as finite sampling. Wilson's work supplies uncertainty bounds for simulation estimates.

Historical source locator: University of York, [Fermat and Pascal on Probability](https://www.york.ac.uk/depts/maths/histstat/pascal.htm). Search indexed the material, but direct retrieval returned 403; no implementation depends on unexamined details of those letters.

Primary mathematical references consulted:

1. Kyle Siegrist, [Lotteries](https://www.randomservices.org/random/games/Lotteries.html), with his [Hypergeometric Distribution](https://www.randomservices.org/random/urn/Hypergeometric.html) chapter. Direct lottery and independent bonus-pool model.
2. Charles M. Grinstead and J. Laurie Snell, [Introduction to Probability](https://math.dartmouth.edu/~prob/prob/NEW/amsbookwithlinks.mac.pdf), especially combinations, conditional probability, expected value and independent trials.
3. NIST/SEMATECH, [Confidence intervals](https://www.itl.nist.gov/div898/handbook/prc/section2/prc241.htm), Wilson score interval, including boundary limitations of a naive normal interval.

## Implemented mathematics

Let N be the main pool and k the number selected and drawn. There are C(N,k) equiprobable unordered outcomes. The binomial coefficient is calculated by exact BigInt recurrence. The count of outcomes with exactly r main matches is C(k,r) C(N-k,k-r). Divide by C(N,k) to obtain the hypergeometric probability.

For a second independent pool of M with j selections, multiply the main match probability by the analogous second-pool probability. For one bonus drawn from the remaining main pool, conditional on r main matches, the chance the ticket contains that bonus is (k-r)/(N-k). This is distinct from a separately chosen PowerBall.

For U unique complete lines, the event of matching all selected numbers has probability U / [C(N,k) C(M,j)]. Complete-line jackpot events are disjoint. For a main-only target, deduplicate the main combinations before counting, even when secondary numbers differ. Lower-match events for different tickets overlap, so applying 1-(1-p)^U within a single draw is generally wrong. The worker enumerates the union exactly when the state space and work budget permit; otherwise it samples independent complete draws and tests whether ANY line meets the target.

Independent repeated draws permit the complement calculation 1-(1-p)^d. Numerically stable log1p/expm1 are used. The worker reports a Wilson 95% interval for Monte Carlo estimates, with explicit zero-hit handling. The interval describes simulation uncertainty, not uncertainty about tomorrow's particular outcome. Rare-event coverage can have substantial relative uncertainty. Jackpot probability never depends on simulation.

Expected return uses a sum of probability times assumed payout for mutually exclusive outcome categories, minus required entry cost. It is a user-defined scenario, not an estimate of actual pari-mutuel payouts, roll-downs, shared jackpots or profit. Prerequisite game payouts are intentionally excluded from single add-on scenarios and the UI states this.

## Generation and constraints

Web Crypto provides unsigned 32-bit integers. Rejection sampling discards values outside the largest multiple of the desired range, avoiding modulo bias. Partial Fisher-Yates selection samples without replacement. Tickets are deduplicated. The random mode is uniform over valid constrained combinations. Include/exclude preferences cannot improve a single line's unconditional winning probability.

Spread mode evaluates 32 candidates against previous selections and chooses the candidate with the lowest sum of squared main-number overlaps. It is deliberately labelled a heuristic. It does not solve a globally optimal covering design and does not promise a higher probability for every match threshold.

A full wheel enumerates every k-subset of a supplied pool. Its main-number guarantee is conditional on all winning main numbers being in that pool. A secondary-pool selection is fixed across its lines. The system never treats a wheel as free extra coverage.

Historical counts are descriptive only. Under the fair independent draw assumption, observing past draws does not change the conditional distribution of the next one. A frequency fluctuation is not itself evidence of an exploitable physical bias. No hot/cold numbers, numerology, Fibonacci weighting, neural prediction, martingale betting or unsupported Bayesian bias model is used.

## Rule research

Nedbank's current lottery FAQ specifies the 1 June 2026 change to 6/52 and PowerBall 1/16, the same-number add-on structure and entry dependencies. Standard Bank independently corroborates the ranges, six game names, retirement of the Daily Lotto add-on, and current price schedule. These current primary retailer sources supersede stale 6/58 and 1/20 search results. Current all-game cost is R28 per line before channel fees, including all required base and add-on entries.

- [Nedbank FAQ](https://personal.nedbank.co.za/bank/digital-banking/needs/buy/lottery-faqs.html)
- [Standard Bank guide](https://www.standardbank.co.za/southafrica/personal/products-and-services/ways-to-bank/help-centre/play-lotto)

The operator homepage could not be retrieved (403). Accordingly, the UI reports the retailer-based verification date and never claims live official results or a verified official prize table. Historical presets are separated from current entries. Custom formats are user-defined and not represented as verified foreign lottery rules.

## Confidence and limitations

Confidence comes from independent enumeration of small sample spaces, known exact denominator checks, normalized outcome distributions, boundary tests and correct dependence handling. It does not imply confidence that any selected number set will win. Browser end-to-end and visual QA were unavailable; engine and worker tests and rendered PDF inspection were completed.
