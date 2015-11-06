# Works Codex
An Android modules that inspired by Wordpress architecture. The name is a huge misnomer since Codex is not actually the name of Wordpress architecture, but rather their documentation site, but I've been using and mentioning the name to anyone I meet, so i guess the name stay.

The works is hugely inspired by Square Otto and it is a huge modification of Otto.

Huge credits to [Square Otto](https://github.com/square/otto) and [Guava
](https://github.com/google/guava)

## Conception
You may want to read the [Conception of the framework](docs/Conception.md) to understand the base concept why I do it this way.

## Usage

The very basic of the framework is rather than having so many definition of callback interfaces and coupling your components with each other, Codex behave as centralised messaging center to send and receive events between components.

### Property and PropertySubscriber

Same idea with Square Otto and Guava EventBus, property is an important part of the frameworks. Main difference is rather than to only have one type of Java class can be defined as one Property, Codex allows you to defines Property by name.

The premise of Property and PropertySubscriber are:

1. An instance of class A which defines a **Property** for P is registered, and an instance of class B which defines a **PropertySubscriber** for P is registered and it receive the value of **Property** P as well when the updates is being called
2. Class B should have and need to treat the default value if there is no instance of class A is registered
3. If instance of class B is registered first and when instance of class A registered, the **Property** P will be send to the **PropertySubscriber**
4. However if later on class A get unregistered, it is up to the developer to update Property P value before unregistering, or just let the **PropertySubscriber** holds on the last value

### Action and ActionHook

Action is actually a derivative of **Property** and **PropertySubscriber**, but instead of having a default value, **Action** doesn't. A method which annotated with **PropertySubscriber** can only have one parameter, where as **ActionHook** can have a many as parameters defines as the number of parameters that will be send by the **Action**.

The premise of Action and ActionHook

1. The annotation **Action** is for documentation purpose rather than have any actual meaning in the operation
2. Action is being send with **startAction(name, Object ... args)** and instance having their class method annotated with **ActionHook** with same name and same number of arguments will be called
3. Arguments count is being used, but not the argument types, which mean you cannot have a overloaded action with same name
4. Different with **Property**, an instance with same **ActionHook** name added after or during action sequence will not be called

## Examples
