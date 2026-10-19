package za.co.lottolab.studio;

/** Offline, plain-language reference for every main LottoLab workflow. */
public final class HelpLibrary {
  private HelpLibrary() {}

  public static final String[][] CHAPTERS = {
    {"Start here", "Your first three steps",
      "1. On Home, choose your lottery game.\n\n"
      + "2. Look at the most recent result and the short statistics beneath it.\n\n"
      + "3. Choose Check my numbers, Pick some numbers or Browse results.\n\n"
      + "You do not need to sign in. Once the history has loaded, the app works offline. LottoLab is for adults aged 18 and over."},
    {"Choose a lottery game", "Lotto, PowerBall, Daily Lotto and more",
      "Tap the game name near the top of Home. Select the game you want to explore.\n\n"
      + "The app keeps Lotto and PowerBall separate and respects when game rules changed.\n\n"
      + "Some older games are marked Archive only. These can be researched, but are not currently offered as new selections."},
    {"Home and Smart Insights", "Understand the numbers at a glance",
      "Home shows the latest result stored on your phone and a simple summary of the last 30 available draws for the selected game.\n\n"
      + "Most seen means one number appeared more often in those saved draws. Least seen means it appeared less often. Last draw balance shows how many were odd or even.\n\n"
      + "These counts are calculated on your phone. They are not predictions, promises or secret winning formulas. A fair lottery gives every valid combination the same jackpot chance."},
    {"Check my numbers", "See what matched",
      "Open the menu and choose Check a ticket. Pick the game, the date of the draw and your main numbers. For PowerBall also enter the separate PowerBall. Tap the check button.\n\n"
      + "If that draw has not been stored, LottoLab says the result is unknown. It will never count a missing draw as a loss.\n\n"
      + "Matches are only an aid. Confirm any actual prize with the official lottery operator."},
    {"Pick and save numbers", "Use the Quick Pick and save favourites",
      "Choose Pick numbers to make a random line. Advanced controls let you keep or leave out certain numbers.\n\n"
      + "Save a favourite to reuse that line later. A favourite is not proof of a purchased ticket. To track a draw you played, save a dated ticket in My Numbers.\n\n"
      + "Random or patterned combinations have no special advantage over other valid lines."},
    {"Look through old draws", "Find past results and explore patterns",
      "Open Results to see the draw archive. You can filter by game and date, or search for numbers.\n\n"
      + "Open Look at past numbers for most seen, least seen, last seen, pairs and draw patterns.\n\n"
      + "Where a draw is missing or came from a third-party archive, LottoLab shows the available data rather than pretending the history is complete."},
    {"Compare my numbers", "Research up to six lines",
      "Choose Compare my lines from the menu. Add up to six number combinations. LottoLab counts how many of the saved draws contained those numbers and shows each line separately.\n\n"
      + "If a combination matched in the past, that does not mean it is more likely to match in a future independent draw."},
    {"Understand advanced tools", "Systems, coverage and chance",
      "Build a full system: makes all complete lines from a larger set of chosen numbers. It may produce many lines and can cost much more.\n\n"
      + "Line coverage: shows repeated and overlapping selections.\n\n"
      + "Explore the odds: shows mathematical chances, not a forecast.\n\n"
      + "Randomness experiment: creates pretend draws for learning only; these are not real results."},
    {"Plan spending", "Keep costs clear",
      "Go to My Numbers or use Plan my spending in the menu. Enter how many entries you are considering and a monthly limit.\n\n"
      + "A planned entry is not a purchased ticket. Only mark it purchased when you have really bought it. Record bundled entries only once.\n\n"
      + "Lottery play is 18+. Decide what you can afford to lose before you play. Stop if it becomes uncomfortable."},
    {"Offline use and updates", "Works without mobile data or Wi-Fi",
      "LottoLab keeps downloaded and bundled draw results on your phone in a local database. You can search, analyse, save, check available dates and export while offline.\n\n"
      + "When internet returns, the app can automatically check for newer results. Android may delay background checks to save battery.\n\n"
      + "To check now, open the side menu, choose Update results and tap Refresh results. If an update fails, existing history is not deleted."},
    {"Back up your records", "Keep saved lines and tickets safe",
      "Open Data and settings from the side menu. Choose Save a backup file. Save the exported file somewhere private.\n\n"
      + "Restore a backup merges saved lines and tickets without removing existing ones. Export CSV for draw tables and PDF for readable reports.\n\n"
      + "A backup is especially important before uninstalling or changing phones."},
    {"Results and source labels", "Know what the app has actually checked",
      "Bundled archive means a historical record came with the app. Downloaded feed means it was retrieved online. Imported / entered means you or a file added it.\n\n"
      + "Sources may disagree or be incomplete. LottoLab keeps correction history where numbers change. Verify important results with the official operator.\n\n"
      + "LottoLab does not confirm cash prizes or provide a guaranteed way to win."}
  };

  public static final String[][] FAQ = {
    {"Does LottoLab work without internet?", "Yes. The bundled archive and any results already saved on the phone can be used offline. New draws require a connection or manual entry."},
    {"Are the Smart Insights powered by a live AI?", "No. Smart Insights uses clear, repeatable calculations on the draw history saved on your phone. It does not call a remote AI or predict winners."},
    {"Can the app predict the next winning numbers?", "No. Independent, fair lottery draws cannot be predicted from old results. Hot, cold and overdue labels only describe what has already happened."},
    {"How do I refresh results?", "Open the side menu, choose Update results, then tap Refresh results. You can also switch on automatic updates in Settings."},
    {"Why is a result missing?", "The archive may not include that date yet, the selected game or rule period may differ, or your phone has been offline. A missing result is unknown, not a loss."},
    {"What does hot or cold mean?", "Hot means appeared more often in the chosen past window. Cold means appeared less often. Neither changes the probability in the next fair draw."},
    {"Why do some games have different number ranges?", "The South African game rules changed in June 2026. LottoLab separates the earlier and current periods so statistics do not mix incompatible number pools."},
    {"What is the difference between favourites and tickets?", "A favourite stores reusable numbers. A dated ticket stores numbers for a specific draw. Only mark a dated ticket purchased if you really paid for it."},
    {"Does it tell me how much I won?", "No. Match counts help you research a ticket but are not an authenticated prize payout. Check any winnings with the official operator."},
    {"Do I need an account or subscription?", "No. Basic use and offline research work without an account or paid API key."},
    {"Why did the automatic update stop?", "Check internet, battery restrictions and the data source in Settings. The source service itself may be temporarily unavailable. Existing results stay on the phone."},
    {"Will I lose my data when updating the APK?", "An in-place update signed with the same app certificate should keep your data. Before reinstalling or changing phones, export a backup. Never uninstall until your saved records are protected."},
    {"What age is LottoLab for?", "Lottery participation is for adults aged 18 or older. The simple instructions are designed for clarity, not for encouraging children to gamble."}
  };
}
