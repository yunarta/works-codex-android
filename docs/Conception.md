# Conception
[tl;dr] During my working days, I've been trying understand what defines you as a good programmer. When you ask your fellow programmer, each one of them will have different point of view.

But when I see from organization point of few, it's easy to understand. When someone left a company, and you cannot continue their work, you will definitely grumbling "this code sucks", "lacks of documentation and comment", etc etc. The next programmer continue your work will most likely have same idea.

Below are most situation that I usually meet

* You just need to add simple function on certain layer depth on the flow, and you cannot comprehend which controller you need to use or even where to find them. (unless they define everything as singleton)
* There a huge number of listener and call chains, and you don't dare to sever and add things in.
* If you need to remove or change certain features entirely, this is where things usually gone worse as coupling between module might be undetectable.
* And so on

But when someone come to you and ask you, I need you to make an app to use Facebook REST API, then it always almost easy for you to writes the code, because what you are facing is just a straight forward INPUT, PROCESS, and OUTPUT from the REST API

You don't have to care where is the controller to add or remove friends, they just a URL and set of data for you. You don't have to care when you are in the 7th level of your layer and you need to like this post, they just a URL and set of data for you as well.

Imagine, if every layer of your module is REST API, placed on servers running on your own local machine, calling this function will be almost as fast as you call functions in your own application.

To be continue ...