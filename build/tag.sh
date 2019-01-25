#!/usr/bin/env bash

set -e

echo 'Fetching tag from remote...'
git tag -l | xargs git tag -d
git fetch --tags

# TODO Find a way to store the output
if ! git describe --exact-match 2>/dev/null; then
  echo 'Not tag found...'

  last_tag=`git describe --abbrev=0 --tags`
  current_version=${last_tag#'v'}

  echo "Current version ${current_version}"

  #replace . with space so can split into an array
  current_version_parts=(${current_version//./ })

  #get number parts and increase last one by 1
  current_version_major=${current_version_parts[0]}
  current_version_minor=${current_version_parts[1]}
  current_version_build=${current_version_parts[2]}

  next_version_build=$((current_version_build+1))
  next_version="$current_version_major.$current_version_minor.$next_version_build"
  next_tag="v${next_version}"

  echo "Tagging the current commit with ${next_tag}"

  git tag -a ${next_tag} -m "Release version "${next_version}

  echo "Pushing tag ${next_tag} to origin"
  git push origin ${next_tag}

else
  echo 'Tag found, no tag will be add'
fi
