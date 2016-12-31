Hello UnifyID!

HOW TO BUILD

Simply open the project in Android Studio and hit run.

WHAT WORKS, WHAT DOESN'T

My program successfully takes 10 snapshots spaced half a second apart and stores these snapshots into 10 separate files in internal storage.

The encryption/decryption system I intended to implement is not yet functioning, although I plan to remedy that ASAP, if for nothing but my own stubbornness.

FURTHER CONSIDERATIONS

I had a few small issues during development (e.g. making sure CameraPreview was ready before taking picture, working through readDataFromFile to verify my results) that took up a good chunk of time. However these probably aren't worth much consideration.

Something I noticed looking through the Android docs was that the two "standard locations you should consider as a developer" when storing media, Environment.getExternalStoragePublicDirectory and Context.getExternalFilesDir, were both low-security. Other applications were free to "read, change, and delete" these files. In order to get around this, I decided some sort of encryption would be necessary. I ended up chosing to store the data in internal storage where only the application itself could access it and encrypting the data.

Something I would consider in the future is perhaps having a set "blur" as part of a persons identification. The "blur" would just be an expected level of additional encryption. My thinking is that if all the system expects is an entirely unencrypted set of data representing a person's face, then it may be vulnerable to people simply pulling up photos of you and recreating the data from that (I assume that because of the emphasis on security in the challenge, the data of one's face is somehow valuable/should not be shared). However, if the system expects a data that has been "blurred" by another layer of encryption, then this issue is removed. I have no clue how realistic this worry is but would love to learn about it.

I also have a few ideas with regards to the overall facial recognition concept. One is an initial parse through the user's gallery upon installation that runs scans on their existing photos in order cut out the need to hold still in front of the camera for a few seconds entirely. Even if those few seconds don't represent a huge cost to the user, any improvement in experience can make a difference. If nothing else, those scans could be used to reinforce the data from the live scan. Another idea was maybe having additional scans for different angles of the head. My reason for this comes from my experience with Snapchat; in order for the app to recognize your face and unlock certain filters, a good amount of focus/positioning is necessary. If the user is using facial recognition to unlock their phone, they likely don't want to be bothered by constantly having to hold their phone up and carefully center their face in order to be recognized. I do understand that your product takes in a variety of input factors, and so realize that the additional scans may not be necessary.

All in all, I enjoyed the challenge and will continue to tinker with it when I get the opportunity.
