# TouchBase
Android Messenger Application

By Andrew Garner
Published June 2015

This application was my senior project for my Computer Science Degree at Walla Walla University in June of 2015.

This app allows messaging between users of the app. It is currently only available for Android.Users log in using a Google account which is logged in on the device. The app communicates with a webserver and then receives responses over an https connection or through Google Cloud Messaging. The app is designed for ease of use and simplicity. As such, there are no over the top features or distracting graphics, and the interface is easy and intuitive. The three main pages of the app are all easily accesed by swiping. All but the log in and profile page are contained within one activity. 


--------------------------------------------

The app has three main pages: Finder, Conversation List, and Conversation. They are all fragments contained within a fragment that acts as a pager for them, while also facilitating communication between the pages and to the main activity. There is also a sign in activity.

SigninActivity - uses a local google account to signin. Only the person's name, email, and photo URL are taken by the app. The app then calls to the webserver to check if that user has an account. If not, one is created. If there is an account, messages and friends are returned to the app. Once everything is set up and logged in, we go to the main activity

MainActivity - master activity that handles interactions and key elements of the app. It sets up the fragment pager and handles rotation, incoming messages, and communication between parts of the app. 

Pager Fragment - holds the three main fragments: Finder, Convo list, and conversation. It also provides functions to allow communication between fragments and to and from the main activity. All of the three pages below can be accessed by simply swiping between them. No opening of activities necessary. This allows for a quick, fluid, and intuitive interface.

Finder Fragment: Used for searching and displaying friends and requests. There is a searchbar that searches users on the webserver.  users are displayed in a listview. Requests and friends are shown in the main friends page, and search results will show if a person is a friend or not. Then there is a separate page in the fragment that only shows requests. When a friend is tapped, it opens the conversation page and allows messaging. If a user who isn't a friend is tapped, it opens a dialog to send a friend request. If a request is tapped, it shows the message with it and allows confirmation or denial of the request. When on this page, the activity is set to adjust nothing so that it doesn't refresh pictures when the keyboard is open.

Conversation List Fragment: Shows all active conversations that the user is a part of. It displays the most recent message in a conversation, and is ordered by which conversations are the most recent. If a message is new and unread, the text is set to bold. Tapping on a conversation opens it in the Conversation page.

Conversation Fragment: Uses a listview to display messages to and from a particular person. An edit text is used to send messages.

