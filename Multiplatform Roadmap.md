# Multiplatform Roadmap

As kotlin multiplatform (and compose multiplatform) becomes more and more popular, the want to
multiplatform this project grows stronger and stronger.

## Things needed to multiplatform this project

- Compose m3 alpha needs to go stable in multiplatform
- Need a way to read external sources.
    - Research needs to be done on desktop and ios.
    - Desktop might be able to use apks...probably not though
- Firebase
    - Maybe? We'd need to do all the different variants too.
        - https://github.com/Tweener/passage?
      - https://github.com/mirzemehdi/KMPAuth?
- Notifications
- Workers
    - Maybe only for ios and desktop?
- Icon getting
- Need to figure our initial setup like what is done in the application class
- Better handling of large screen devices
- Github Actions workflows
- Gotta see if protobuf works in multiplatform
- Version getting
- Image loaders would get cut down
- Some libraries would get cut most probably
- Full removal of gson
- BiometricUtils would need some research
- Translation models would need another look
- MediaPlayer
    - https://github.com/kdroidFilter/ComposeMediaPlayer
    - Casting?
- Deeplinking
- Readme updates

## Things that can be done now

- [x] Models and FavoritesDatabase can be converted to multiplatform now
  - [x] FavoritesDatabase
  - [x] Datastore
    - [x] Protobuf
    - [x] Need to figure out specific apps protobuf. Might need a later change.
  - [ ] Models
    - Uses some Java libraries, so need to figure out a way to keep some of that while also not
      affecting any other sources
- [ ] Screens that do not have m3 alpha components can go into a multiplatform module
- [ ] Start removal of gson
- [ ] ViewModels could probably be moved into the kmp module

