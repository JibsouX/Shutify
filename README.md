<div align="center">
  <h1>Shutify</h1>
  <br>
</div>

**Shutify** is an LSPosed module for Spotify that blocks ads at the network level and removes ad sections from the home and browse screens.

No interaction needed — it works silently in the background.

### Patches included
- **Ad Blocker**
- **Unlock all Premium features** - based on v260303 of [xposed spotify](https://github.com/chsbuffer/ReVancedXposed_Spotify)

Unlike [Shushify](https://github.com/jibsoux/Shushify), ads never load in the first place.

---

### The Impact of Server-Side Consistency Checks


Starting from late January 2026, the server has implemented a new verification logic 
that enforces strict **dual-sync checks** for account attributes and configuration data. 
The server now cross-references your account attributes (such as Subscription Type) and 
core configuration data in real-time. If client-side modifications or suppressed logics are detected, 
the server will immediately forcibly terminate the session.

**To prevent frequent logouts, we have adjusted the patches to prioritize usability. **

**Consequently:**

- ~~Audio and visual ads will now appear.~~
- Non-functional Download button now visible.

Remember: if you are not paying for the product, **you** are the product.

## ⭐ Credits

[DexKit](https://luckypray.org/DexKit/en/): a high-performance dex runtime parsing library.  
[ReVanced](https://revanced.app): Continuing the legacy of Vanced at [revanced.app](https://revanced.app)  
[Original project by chsbuffer](https://github.com/chsbuffer/ReVancedXposed_Spotify)

## 🚀 Installation (simple way)

Install [Shutify APK](https://github.com/jibsoux/Shutify/releases/latest).
Install [MochiCloner](https://mochicloner.com/), activate the module (checkbox in module menu). Then clone Spotify.