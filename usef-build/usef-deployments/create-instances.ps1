#
# Create and deploy Common Reference Operators
#
ForEach ($number in 0..0 ) { 
  ${cro-instance} = "{0:D3}" -f $number
  ./create-role-instance.ps1 cro ${cro-instance}
}
#
# Create and deploy Balance Responsible Parties
#
ForEach ($number in 0..1 ) { 
  ${brp-instance} = "{0:D3}" -f $number
  ./create-role-instance.ps1 brp ${brp-instance}
}
#
# Create and deploy Distribution System Operators
#
ForEach ($number in 0..4 ) { 
  ${dso-instance} = "{0:D3}" -f $number
  ./create-role-instance.ps1 dso ${dso-instance}
}
#
# Create and deploy Aggregators
#
ForEach ($number in 0..9 ) { 
  ${agr-instance} = "{0:D3}" -f $number
  ./create-role-instance.ps1 agr ${agr-instance}
}
