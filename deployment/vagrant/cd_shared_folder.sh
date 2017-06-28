#!/bin/bash

if ! grep -q "cd $1" "/home/vagrant/.bashrc"; then
  echo "cd $1" >> "/home/vagrant/.bashrc"
fi
